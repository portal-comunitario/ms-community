package com.portalcomunitario.mscommunity.post;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock private PostRepository postRepository;

    private PostService service;

    @BeforeEach
    void setUp() {
        service = new PostService(postRepository);
    }

    private Post post(PostEstado estado) {
        Post p = new Post();
        p.setId(UUID.randomUUID());
        p.setTitulo("Corte de agua");
        p.setContenido("Manana no habra suministro");
        p.setAuthorEmail("autor@x.com");
        p.setTipo(PostTipo.NOTICIA);
        p.setEstado(estado);
        return p;
    }

    @Test
    @DisplayName("findAll: un admin de comunidad ve TODOS los posts (findAllByOrderByCreatedAtDesc)")
    void findAll_admin_veTodos() {
        when(postRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(
                post(PostEstado.PENDIENTE),
                post(PostEstado.APROBADO)
        ));

        List<PostResponse> res = service.findAll("COMMUNITY_ADMIN");

        assertThat(res).hasSize(2);
        verify(postRepository).findAllByOrderByCreatedAtDesc();
        verify(postRepository, never()).findByEstadoOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("findAll: un admin de plataforma también ve TODOS los posts")
    void findAll_platformAdmin_veTodos() {
        when(postRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(
                post(PostEstado.PENDIENTE)
        ));

        List<PostResponse> res = service.findAll("PLATFORM_ADMIN");

        assertThat(res).hasSize(1);
        verify(postRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("findAll: un vecino solo ve los posts APROBADO")
    void findAll_vecino_soloAprobados() {
        when(postRepository.findByEstadoOrderByCreatedAtDesc(PostEstado.APROBADO)).thenReturn(List.of(
                post(PostEstado.APROBADO)
        ));

        List<PostResponse> res = service.findAll("VECINO");

        assertThat(res).hasSize(1);
        assertThat(res.get(0).estado()).isEqualTo("APROBADO");
        verify(postRepository).findByEstadoOrderByCreatedAtDesc(PostEstado.APROBADO);
        verify(postRepository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("findPendientes: consulta únicamente la cola de moderación (PENDIENTE)")
    void findPendientes_soloPendientes() {
        when(postRepository.findByEstadoOrderByCreatedAtDesc(PostEstado.PENDIENTE)).thenReturn(List.of(
                post(PostEstado.PENDIENTE)
        ));

        List<PostResponse> res = service.findPendientes();

        assertThat(res).hasSize(1);
        assertThat(res.get(0).estado()).isEqualTo("PENDIENTE");
        verify(postRepository).findByEstadoOrderByCreatedAtDesc(PostEstado.PENDIENTE);
    }

    @Test
    @DisplayName("create: el post nace PENDIENTE con el autor y tipo recibidos")
    void create_nacePendiente() {
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostRequest req = new PostRequest("Titulo", "Contenido", "noticia",
                -33.4, -70.6, "Calle 1");
        PostResponse res = service.create(req, "autor@x.com");

        assertThat(res.estado()).isEqualTo("PENDIENTE");
        assertThat(res.tipo()).isEqualTo("NOTICIA");
        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        assertThat(captor.getValue().getAuthorEmail()).isEqualTo("autor@x.com");
    }

    @Test
    @DisplayName("create: tipo inválido cae al valor por defecto ANUNCIO")
    void create_tipoInvalido_usaAnuncio() {
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostRequest req = new PostRequest("t", "c", "tipo-que-no-existe", null, null, null);
        PostResponse res = service.create(req, "autor@x.com");

        assertThat(res.tipo()).isEqualTo("ANUNCIO");
    }

    @Test
    @DisplayName("aprobar: cambia el estado del post a APROBADO")
    void aprobar_cambiaEstado() {
        UUID id = UUID.randomUUID();
        Post p = post(PostEstado.PENDIENTE);
        p.setId(id);
        when(postRepository.findById(id)).thenReturn(Optional.of(p));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostResponse res = service.aprobar(id);

        assertThat(res.estado()).isEqualTo("APROBADO");
    }

    @Test
    @DisplayName("findById: un id inexistente lanza 404")
    void findById_noExiste_404() {
        UUID id = UUID.randomUUID();
        when(postRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no encontrado");
    }

    @Test
    @DisplayName("delete: si el post no existe lanza 404 y no borra nada")
    void delete_noExiste_404() {
        UUID id = UUID.randomUUID();
        when(postRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no encontrado");
        verify(postRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("rechazar: cambia el estado del post a RECHAZADO")
    void rechazar_cambiaEstado() {
        UUID id = UUID.randomUUID();
        Post p = post(PostEstado.PENDIENTE);
        p.setId(id);
        when(postRepository.findById(id)).thenReturn(Optional.of(p));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostResponse res = service.rechazar(id);

        assertThat(res.estado()).isEqualTo("RECHAZADO");
    }

    @Test
    @DisplayName("aprobar: un id inexistente lanza 404")
    void aprobar_noExiste_404() {
        UUID id = UUID.randomUUID();
        when(postRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.aprobar(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no encontrado");
    }

    @Test
    @DisplayName("delete: si el post existe lo borra")
    void delete_ok() {
        UUID id = UUID.randomUUID();
        when(postRepository.existsById(id)).thenReturn(true);

        service.delete(id);

        verify(postRepository).deleteById(id);
    }

    @Test
    @DisplayName("create: tipo nulo cae al valor por defecto ANUNCIO")
    void create_tipoNull_usaAnuncio() {
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostRequest req = new PostRequest("t", "c", null, null, null, null);
        PostResponse res = service.create(req, "autor@x.com");

        assertThat(res.tipo()).isEqualTo("ANUNCIO");
    }

    @Test
    @DisplayName("create: tipo en blanco cae al valor por defecto ANUNCIO")
    void create_tipoBlank_usaAnuncio() {
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostRequest req = new PostRequest("t", "c", "   ", null, null, null);
        PostResponse res = service.create(req, "autor@x.com");

        assertThat(res.tipo()).isEqualTo("ANUNCIO");
    }

    @Test
    @DisplayName("rechazar: un id inexistente lanza 404")
    void rechazar_noExiste_404() {
        UUID id = UUID.randomUUID();
        when(postRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rechazar(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no encontrado");
    }

    @Test
    @DisplayName("findById: un id existente devuelve el post mapeado")
    void findById_existente_devuelve() {
        UUID id = UUID.randomUUID();
        Post p = post(PostEstado.APROBADO);
        p.setId(id);
        when(postRepository.findById(id)).thenReturn(Optional.of(p));

        PostResponse res = service.findById(id);

        assertThat(res.estado()).isEqualTo("APROBADO");
    }

}
