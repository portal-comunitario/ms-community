package com.portalcomunitario.mscommunity.aviso;

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
class AvisoServiceTest {

    @Mock private AvisoRepository repository;

    private AvisoService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new AvisoService(repository);
    }

    private Aviso aviso(AvisoEstado estado, String autor) {
        Aviso a = new Aviso();
        a.setId(UUID.randomUUID());
        a.setTitulo("Se vende bicicleta");
        a.setDescripcion("Casi nueva");
        a.setCategoria(AvisoCategoria.COMPRA_VENTA);
        a.setAuthorEmail(autor);
        a.setEstado(estado);
        a.setResuelto(false);
        return a;
    }

    @Test
    @DisplayName("findAll: un admin de comunidad ve TODOS los avisos (findAllByOrderByCreatedAtDesc)")
    void findAll_admin_veTodos() {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(
                aviso(AvisoEstado.PENDIENTE, "a@x.com"),
                aviso(AvisoEstado.APROBADO, "b@x.com")
        ));

        List<AvisoResponse> res = service.findAll("COMMUNITY_ADMIN");

        assertThat(res).hasSize(2);
        verify(repository).findAllByOrderByCreatedAtDesc();
        verify(repository, never()).findByEstadoOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("findAll: un admin de plataforma también ve TODOS los avisos")
    void findAll_platformAdmin_veTodos() {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(
                aviso(AvisoEstado.PENDIENTE, "a@x.com")
        ));

        List<AvisoResponse> res = service.findAll("PLATFORM_ADMIN");

        assertThat(res).hasSize(1);
        verify(repository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("findAll: un vecino solo ve los avisos APROBADO")
    void findAll_vecino_soloAprobados() {
        when(repository.findByEstadoOrderByCreatedAtDesc(AvisoEstado.APROBADO)).thenReturn(List.of(
                aviso(AvisoEstado.APROBADO, "b@x.com")
        ));

        List<AvisoResponse> res = service.findAll("VECINO");

        assertThat(res).hasSize(1);
        assertThat(res.get(0).estado()).isEqualTo("APROBADO");
        verify(repository).findByEstadoOrderByCreatedAtDesc(AvisoEstado.APROBADO);
        verify(repository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("findPendientes: consulta únicamente la cola de moderación (PENDIENTE)")
    void findPendientes_soloPendientes() {
        when(repository.findByEstadoOrderByCreatedAtDesc(AvisoEstado.PENDIENTE)).thenReturn(List.of(
                aviso(AvisoEstado.PENDIENTE, "a@x.com")
        ));

        List<AvisoResponse> res = service.findPendientes();

        assertThat(res).hasSize(1);
        assertThat(res.get(0).estado()).isEqualTo("PENDIENTE");
        verify(repository).findByEstadoOrderByCreatedAtDesc(AvisoEstado.PENDIENTE);
    }

    @Test
    @DisplayName("create: el aviso nace PENDIENTE, sin resolver y con el autor recibido")
    void create_naceePendiente() {
        when(repository.save(any(Aviso.class))).thenAnswer(inv -> inv.getArgument(0));

        AvisoRequest req = new AvisoRequest("Clases de guitarra", "Particulares",
                "servicio", -33.4, -70.6, "Calle 1", null, "+56 9");
        AvisoResponse res = service.create(req, "autor@x.com");

        assertThat(res.estado()).isEqualTo("PENDIENTE");
        assertThat(res.resuelto()).isFalse();
        ArgumentCaptor<Aviso> captor = ArgumentCaptor.forClass(Aviso.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAuthorEmail()).isEqualTo("autor@x.com");
        assertThat(captor.getValue().getCategoria()).isEqualTo(AvisoCategoria.SERVICIO);
    }

    @Test
    @DisplayName("create: categoría inválida cae al valor por defecto SERVICIO")
    void create_categoriaInvalida_usaServicio() {
        when(repository.save(any(Aviso.class))).thenAnswer(inv -> inv.getArgument(0));

        AvisoRequest req = new AvisoRequest("t", "d", "categoria-que-no-existe",
                null, null, null, null, null);
        AvisoResponse res = service.create(req, "autor@x.com");

        assertThat(res.categoria()).isEqualTo("SERVICIO");
    }

    @Test
    @DisplayName("marcarResuelto: el propio autor puede marcar su aviso como resuelto")
    void marcarResuelto_autor_ok() {
        UUID id = UUID.randomUUID();
        Aviso a = aviso(AvisoEstado.APROBADO, "autor@x.com");
        a.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(a));
        when(repository.save(any(Aviso.class))).thenAnswer(inv -> inv.getArgument(0));

        AvisoResponse res = service.marcarResuelto(id, "AUTOR@x.com", "VECINO");

        assertThat(res.resuelto()).isTrue();
    }

    @Test
    @DisplayName("marcarResuelto: un vecino distinto del autor recibe 403")
    void marcarResuelto_otroVecino_403() {
        UUID id = UUID.randomUUID();
        Aviso a = aviso(AvisoEstado.APROBADO, "autor@x.com");
        a.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.marcarResuelto(id, "otro@x.com", "VECINO"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("autor");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("update: un admin puede editar el aviso de cualquier vecino")
    void update_admin_ok() {
        UUID id = UUID.randomUUID();
        Aviso a = aviso(AvisoEstado.APROBADO, "autor@x.com");
        a.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(a));
        when(repository.save(any(Aviso.class))).thenAnswer(inv -> inv.getArgument(0));

        AvisoRequest req = new AvisoRequest("Nuevo titulo", "Nueva desc", "arriendo",
                null, null, null, 100000, "contacto");
        AvisoResponse res = service.update(id, req, "admin@x.com", "COMMUNITY_ADMIN");

        assertThat(res.titulo()).isEqualTo("Nuevo titulo");
        assertThat(res.categoria()).isEqualTo("ARRIENDO");
    }

    @Test
    @DisplayName("findById: un id inexistente lanza 404")
    void findById_noExiste_404() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no encontrado");
    }

    @Test
    @DisplayName("aprobar: cambia el estado del aviso a APROBADO")
    void aprobar_cambiaEstado() {
        UUID id = UUID.randomUUID();
        Aviso a = aviso(AvisoEstado.PENDIENTE, "autor@x.com");
        a.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(a));
        when(repository.save(any(Aviso.class))).thenAnswer(inv -> inv.getArgument(0));

        AvisoResponse res = service.aprobar(id);

        assertThat(res.estado()).isEqualTo("APROBADO");
    }

    @Test
    @DisplayName("rechazar: cambia el estado del aviso a RECHAZADO")
    void rechazar_cambiaEstado() {
        UUID id = UUID.randomUUID();
        Aviso a = aviso(AvisoEstado.PENDIENTE, "autor@x.com");
        a.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(a));
        when(repository.save(any(Aviso.class))).thenAnswer(inv -> inv.getArgument(0));

        AvisoResponse res = service.rechazar(id);

        assertThat(res.estado()).isEqualTo("RECHAZADO");
    }

    @Test
    @DisplayName("marcarResuelto: un admin (aunque no sea el autor) puede marcarlo")
    void marcarResuelto_admin_ok() {
        UUID id = UUID.randomUUID();
        Aviso a = aviso(AvisoEstado.APROBADO, "autor@x.com");
        a.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(a));
        when(repository.save(any(Aviso.class))).thenAnswer(inv -> inv.getArgument(0));

        AvisoResponse res = service.marcarResuelto(id, "admin@x.com", "COMMUNITY_ADMIN");

        assertThat(res.resuelto()).isTrue();
    }

    @Test
    @DisplayName("update: el propio autor puede editar su aviso")
    void update_autor_ok() {
        UUID id = UUID.randomUUID();
        Aviso a = aviso(AvisoEstado.APROBADO, "autor@x.com");
        a.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(a));
        when(repository.save(any(Aviso.class))).thenAnswer(inv -> inv.getArgument(0));

        AvisoRequest req = new AvisoRequest("Otro titulo", "Otra desc", null,
                null, null, null, null, null);
        AvisoResponse res = service.update(id, req, "AUTOR@x.com", "VECINO");

        assertThat(res.titulo()).isEqualTo("Otro titulo");
        assertThat(res.categoria()).isEqualTo("SERVICIO");
    }

    @Test
    @DisplayName("update: un vecino que no es el autor recibe 403 y no guarda")
    void update_noAutorNoAdmin_403() {
        UUID id = UUID.randomUUID();
        Aviso a = aviso(AvisoEstado.APROBADO, "autor@x.com");
        a.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(a));

        AvisoRequest req = new AvisoRequest("t", "d", "servicio", null, null, null, null, null);
        assertThatThrownBy(() -> service.update(id, req, "otro@x.com", "VECINO"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("autor");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("delete: el autor puede borrar su aviso")
    void delete_autor_ok() {
        UUID id = UUID.randomUUID();
        Aviso a = aviso(AvisoEstado.APROBADO, "autor@x.com");
        a.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(a));

        service.delete(id, "autor@x.com", "VECINO");

        verify(repository).deleteById(id);
    }

    @Test
    @DisplayName("delete: un vecino distinto del autor recibe 403 y no borra")
    void delete_noAutor_403() {
        UUID id = UUID.randomUUID();
        Aviso a = aviso(AvisoEstado.APROBADO, "autor@x.com");
        a.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.delete(id, "otro@x.com", "VECINO"))
                .isInstanceOf(ResponseStatusException.class);
        verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("create: categoría en blanco cae al valor por defecto SERVICIO")
    void create_categoriaBlank_usaServicio() {
        when(repository.save(any(Aviso.class))).thenAnswer(inv -> inv.getArgument(0));

        AvisoRequest req = new AvisoRequest("t", "d", "   ", null, null, null, null, null);
        AvisoResponse res = service.create(req, "autor@x.com");

        assertThat(res.categoria()).isEqualTo("SERVICIO");
    }

}
