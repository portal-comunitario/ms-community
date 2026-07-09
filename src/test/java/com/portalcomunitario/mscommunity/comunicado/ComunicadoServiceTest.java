package com.portalcomunitario.mscommunity.comunicado;

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
class ComunicadoServiceTest {

    @Mock private ComunicadoRepository repository;

    private ComunicadoService service;

    @BeforeEach
    void setUp() {
        service = new ComunicadoService(repository);
    }

    private Comunicado comunicado(ComunicadoCategoria categoria) {
        Comunicado c = new Comunicado();
        c.setId(UUID.randomUUID());
        c.setTitulo("Corte de agua");
        c.setContenido("Mañana no habrá suministro");
        c.setCategoria(categoria);
        c.setAuthorEmail("dirigente@x.com");
        return c;
    }

    @Test
    @DisplayName("findAll: mapea todos los comunicados en orden descendente")
    void findAll_mapeaTodos() {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(
                comunicado(ComunicadoCategoria.URGENTE),
                comunicado(ComunicadoCategoria.NOTICIA)
        ));

        List<ComunicadoResponse> res = service.findAll();

        assertThat(res).hasSize(2);
        assertThat(res.get(0).categoria()).isEqualTo("URGENTE");
        verify(repository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("create: usa la categoría recibida y conserva el autor")
    void create_categoriaValida() {
        when(repository.save(any(Comunicado.class))).thenAnswer(inv -> inv.getArgument(0));

        ComunicadoRequest req = new ComunicadoRequest("Título", "Contenido", "urgente", "http://img");
        ComunicadoResponse res = service.create(req, "dirigente@x.com");

        assertThat(res.categoria()).isEqualTo("URGENTE");
        ArgumentCaptor<Comunicado> captor = ArgumentCaptor.forClass(Comunicado.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAuthorEmail()).isEqualTo("dirigente@x.com");
        assertThat(captor.getValue().getImagenUrl()).isEqualTo("http://img");
    }

    @Test
    @DisplayName("create: categoría inválida cae al valor por defecto NOTICIA")
    void create_categoriaInvalida_usaNoticia() {
        when(repository.save(any(Comunicado.class))).thenAnswer(inv -> inv.getArgument(0));

        ComunicadoRequest req = new ComunicadoRequest("t", "c", "categoria-que-no-existe", null);
        ComunicadoResponse res = service.create(req, "dirigente@x.com");

        assertThat(res.categoria()).isEqualTo("NOTICIA");
    }

    @Test
    @DisplayName("create: categoría nula/en blanco cae al valor por defecto NOTICIA")
    void create_categoriaEnBlanco_usaNoticia() {
        when(repository.save(any(Comunicado.class))).thenAnswer(inv -> inv.getArgument(0));

        ComunicadoResponse res = service.create(new ComunicadoRequest("t", "c", "  ", null), "d@x.com");

        assertThat(res.categoria()).isEqualTo("NOTICIA");
    }

    @Test
    @DisplayName("update: edita el comunicado existente y aplica la nueva categoría")
    void update_ok() {
        UUID id = UUID.randomUUID();
        Comunicado c = comunicado(ComunicadoCategoria.NOTICIA);
        c.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(c));
        when(repository.save(any(Comunicado.class))).thenAnswer(inv -> inv.getArgument(0));

        ComunicadoResponse res = service.update(id, new ComunicadoRequest("Nuevo", "Cuerpo", "aviso", null));

        assertThat(res.titulo()).isEqualTo("Nuevo");
        assertThat(res.categoria()).isEqualTo("AVISO");
    }

    @Test
    @DisplayName("update: id inexistente lanza 404")
    void update_noExiste_404() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, new ComunicadoRequest("t", "c", "aviso", null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no encontrado");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("findById: id inexistente lanza 404")
    void findById_noExiste_404() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no encontrado");
    }

    @Test
    @DisplayName("delete: si no existe lanza 404 y no borra nada")
    void delete_noExiste_404() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no encontrado");
        verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete: si existe borra por id")
    void delete_existe_borra() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(true);

        service.delete(id);

        verify(repository).deleteById(id);
    }

    @Test
    @DisplayName("findById: id existente devuelve el comunicado mapeado")
    void findById_existente_devuelve() {
        UUID id = UUID.randomUUID();
        Comunicado c = comunicado(ComunicadoCategoria.URGENTE);
        c.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(c));

        ComunicadoResponse res = service.findById(id);

        assertThat(res.categoria()).isEqualTo("URGENTE");
        assertThat(res.titulo()).isEqualTo("Corte de agua");
    }
}
