# Documentación Técnica - Práctica 3: Pruebas TDD y Gestión de Equipos - ToDoList

**Autor:** Iván Lafont  
**Asignatura:** Metodologías Ágiles de Desarrollo de Software (MADS)  
**Versión:** 1.2.0  

---

## 1. Pantalla de la Base de Datos PostgreSQL

A continuación se muestra la estructura final de tablas generada en la base de datos de desarrollo PostgreSQL (`equipos`, `usuarios`, `tareas` y la tabla de unión muchos a muchos `equipo_usuario`):

![Estructura de tablas en PostgreSQL](img/tablas_postgresql.png)

*Las cuatro tablas representan el modelo relacional completo tras incorporar la gestión de equipos y su relación bidireccional Many-to-Many con los usuarios.*

---

## 2. Historia de Usuario 009: Gestionar Pertenencia al Equipo

Esta historia permite a los usuarios autenticados crear nuevos equipos y gestionar su propia afiliación a los mismos (unirse o abandonar).

### Rutas (Endpoints) Definidas
* **`GET /equipos/nuevo`**: Muestra el formulario para crear un nuevo equipo.
* **`POST /equipos/nuevo`**: Procesa la creación de un equipo con su nombre. Valida que el nombre no sea vacío y que no exista otro equipo con el mismo nombre.
* **`POST /equipos/{id}/unirse`**: Añade al usuario conectado como miembro del equipo especificado.
* **`POST /equipos/{id}/salir`**: Elimina al usuario conectado del equipo especificado.

### Clases y Métodos
* **`Equipo` (`madstodolist.model`)**:
  * `addUsuario(Usuario usuario)`: Sincroniza la relación en ambos lados (`equipo.getUsuarios().add(usuario)` y `usuario.getEquipos().add(this)`).
  * `removeUsuario(Usuario usuario)`: Desvincula de forma segura en ambos sentidos (`equipo.getUsuarios().remove(usuario)` y `usuario.getEquipos().remove(this)`).
* **`EquipoService` (`madstodolist.service`)**:
  * `crearEquipo(String nombre)`: Lanza `EquipoServiceException` si el nombre es nulo/vacío o si ya existe un equipo con ese nombre.
  * `eliminarUsuarioDeEquipo(Long idEquipo, Long idUsuario)`: Valida existencia y remueve al usuario del equipo actualizando la relación bidireccional.
  * `usuarioEnEquipo(Long idEquipo, Long idUsuario)`: Comprueba si el usuario forma parte de la lista de miembros.
* **`EquipoController` (`madstodolist.controller`)**:
  * Controladores para cada endpoint con redirecciones y mensajes flash de éxito o alertas de error. Comprueba que el usuario esté debidamente autenticado (`comprobarUsuarioLogeado`).

### Plantillas Thymeleaf
* **`formNuevoEquipo.html`**: Formulario con campo `nombre`, validación de campos obligatorios y renderizado de mensajes de error de negocio.
* **`detalleEquipo.html`**: Muestra dinámicamente el botón verde **"Unirse al equipo"** (si `!esMiembro`) o el botón rojo **"Salir del equipo"** (si `esMiembro`).

### Tests Automatizados
* **TDD en `EquipoTest` y `EquipoServiceTest`**: Batería de 10 tests cubriendo:
  * Desvinculación de usuarios en la entidad `Equipo` (`removeUsuario`).
  * Persistencia en BD de la desvinculación en la tabla `equipo_usuario`.
  * Validación de nombres vacíos y duplicados al crear equipo.
  * Excepciones al añadir usuarios inexistentes o duplicados al equipo.
  * Comprobación del estado de pertenencia con `usuarioEnEquipo`.
* **Tests Web en `EquipoWebTest`**:
  * Comprobación de seguridad (401 si no hay login).
  * Renderizado correcto del botón "Unirse" vs "Salir".
  * Peticiones POST para unirse, salir y crear equipo con redirecciones y flash attributes.

---

## 3. Historia de Usuario 010: Gestión de Equipos (Administrador)

Permite exclusivamente a los usuarios con rol de administrador modificar el nombre de un equipo o eliminarlo del sistema sin afectar a los usuarios registrados.

### Rutas (Endpoints) Definidas
* **`GET /equipos/{id}/editar`**: Muestra el formulario para modificar el nombre del equipo. Solo accesible por administradores.
* **`POST /equipos/{id}/editar`**: Actualiza el nombre del equipo validando que no esté vacío ni colisione con el nombre de otro equipo diferente.
* **`POST /equipos/{id}/eliminar`**: Elimina el equipo indicado desvinculando previamente a sus miembros. Solo accesible por administradores.

### Clases y Métodos
* **`EquipoService` (`madstodolist.service`)**:
  * `modificarEquipo(Long idEquipo, String nuevoNombre)`: Valida que el equipo exista, que el nombre no esté en blanco y que no exista otro equipo con el mismo nombre (permitiendo conservar el propio nombre actual).
  * `eliminarEquipo(Long idEquipo)`: Desvincula a todos los usuarios del equipo iterando sobre una copia defensiva (`new HashSet<>(equipo.getUsuarios())`) e invocando `equipo.removeUsuario(usuario)` antes de llamar a `equipoRepository.delete(equipo)`.
* **`EquipoController` (`madstodolist.controller`)**:
  * `comprobarAdmin()`: Verifica que el usuario tenga sesión iniciada y que `usuario.isAdmin()` sea verdadero; en caso contrario, lanza `UsuarioNoAutorizadoException` (HTTP 401).
  * Métodos `formEditarEquipo`, `editarEquipo` y `eliminarEquipo`.

### Plantillas Thymeleaf
* **`formEditarEquipo.html`**: Formulario con el valor actual precargado y botón de confirmación de cambios.
* **`listaEquipos.html` y `detalleEquipo.html`**: Incorporan condicionalmente (`th:if="${usuario.admin}"`) los botones de **"Editar"** y **"Eliminar"** (este último con diálogo JavaScript de confirmación). Los usuarios estándar no ven estos botones.

### Tests Automatizados
* **TDD en `EquipoTest` y `EquipoServiceTest`**: Batería de 10 tests cubriendo:
  * Modificación de nombre a nivel de entidad.
  * Modificación en servicio y persistencia.
  * Excepciones ante equipo no encontrado o nombre vacío.
  * Prevención de duplicados pero permitiendo mantener el mismo nombre.
  * Comprobación de que eliminar un equipo no borra los usuarios asociados ni en base de datos ni a través del servicio.
  * Excepción ante eliminación de equipo no existente.
* **Tests Web en `EquipoWebTest`**:
  * Denegación de acceso (401) para usuarios no autenticados o no administradores en las rutas `/editar` y `/eliminar`.
  * Verificación de que los botones de administración solo se muestran a usuarios con `usuario.isAdmin() == true`.
  * Ejecución exitosa de edición y borrado con redirección y mensajes flash.

---

## 4. Fragmentos de Código Fuente Destacados

### Desvinculación y Borrado Seguro de Equipos (`EquipoService.java`)
Garantiza la integridad referencial en la base de datos limpiando la tabla intermedia antes de eliminar la fila del equipo:

```java
@Transactional
public void eliminarEquipo(Long idEquipo) {
    Equipo equipo = equipoRepository.findById(idEquipo).orElse(null);
    if (equipo == null) {
        throw new EquipoServiceException("Equipo no encontrado");
    }
    for (Usuario usuario : new HashSet<>(equipo.getUsuarios())) {
        equipo.removeUsuario(usuario);
    }
    equipoRepository.delete(equipo);
}
```

### Control de Autorización de Administrador (`EquipoController.java`)
```java
private UsuarioData comprobarAdmin() {
    UsuarioData usuario = comprobarUsuarioLogeado();
    if (!usuario.isAdmin()) {
        throw new UsuarioNoAutorizadoException();
    }
    return usuario;
}
```
