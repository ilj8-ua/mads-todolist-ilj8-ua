# Documentación Técnica - Práctica 3: Pruebas TDD y Gestión de Equipos - ToDoList

**Autor:** Iván Lafont  
**Asignatura:** Metodologías Ágiles de Desarrollo de Software (MADS)  
**Versión:** 1.2.0  

---

## 1. Introducción y Metodología TDD

En esta tercera práctica se ha ampliado la aplicación **ToDoList** incorporando la funcionalidad completa de **Gestión de Equipos**, aplicando de forma rigurosa la metodología **Test-Driven Development (TDD)** y el flujo de trabajo ágil con **GitHub Issues, ramas de características (`feature branches`) y Pull Requests**.

Las historias de usuario desarrolladas han sido:
1. **Historia 008 - Listado de equipos**: Creación de la entidad `Equipo`, relación Muchos a Muchos (`@ManyToMany`) con `Usuario`, listado alfabético y vista de detalle de miembros.
2. **Historia 009 - Gestionar pertenencia a un equipo**: Creación de nuevos equipos con validación de nombre, y funcionalidad para que los usuarios puedan unirse y abandonar equipos de trabajo.
3. **Historia 010 - Gestión de equipos (administrador)**: Capacidad para que los administradores puedan renombrar equipos existentes y eliminarlos sin borrar a los usuarios asociados.

### Metodología de Desarrollo y Commits Modulares
Se ha seguido estrictamente el ciclo **Red-Green-Refactor**:
* Cada funcionalidad de modelo y servicio se dividió en una batería planificada de **10 tests unitarios y de integración**, implementados y confirmados uno a uno.
* Los commits se mantuvieron pequeños y modulares (un commit por cada test superado con su código mínimo indispensable).
* Trazabilidad completa en GitHub: cada historia se descompuso en dos issues (Servicio/Modelo e Interfaz/Controller), desarrolladas en sus ramas correspondientes (`servicio-*` y `vista-*`) e integradas en `main` mediante Pull Requests.

---

## 2. Nuevas Clases y Métodos Implementados

### Capa de Modelo y DTOs (`madstodolist.model` y `madstodolist.dto`)
* **`Equipo`**:
  * Entidad JPA mapeada a la tabla `equipos` con atributos `id` y `nombre` (único y obligatorio).
  * Relación bidireccional `@ManyToMany` con `Usuario` gestionada mediante la tabla intermedia `equipo_usuario`.
  * Métodos de conveniencia para mantener la sincronización bidireccional: `addUsuario(Usuario)` y `removeUsuario(Usuario)`.
* **`Usuario`**:
  * Incorpora la colección `@ManyToMany(mappedBy = "usuarios") Set<Equipo> equipos` para consultar los equipos a los que pertenece el usuario.
* **`EquipoData`**:
  * DTO para transferir la información de equipos entre las capas de servicio, controlador y vistas, evitando exponer la entidad persistente.

### Capa de Repositorios y Servicios (`madstodolist.repository` y `madstodolist.service`)
* **`EquipoRepository`**:
  * Repositorio Spring Data JPA con métodos de consulta personalizados:
    * `Optional<Equipo> findByNombre(String nombre)`
* **`EquipoService`**:
  * `crearEquipo(String nombre)`: Valida que el nombre no sea nulo ni vacío, comprueba unicidad y persiste la nueva entidad.
  * `recuperarEquipo(Long id)`: Recupera los datos de un equipo lanzando `EquipoServiceException` si no existe.
  * `findAllOrdenadoPorNombre()`: Retorna todos los equipos del sistema ordenados alfabéticamente por nombre.
  * `añadirUsuarioAEquipo(Long idEquipo, Long idUsuario)`: Valida existencia y vincula al usuario en el equipo, lanzando excepción si ya pertenecía al mismo.
  * `eliminarUsuarioDeEquipo(Long idEquipo, Long idUsuario)`: Desvincula al usuario del equipo indicado.
  * `usuarioEnEquipo(Long idEquipo, Long idUsuario)`: Comprueba si un usuario dado es miembro del equipo.
  * `usuariosEquipo(Long idEquipo)` y `equiposUsuario(Long idUsuario)`: Recuperan colecciones de miembros y equipos asociados respectivamente.
  * `modificarEquipo(Long idEquipo, String nuevoNombre)`: Permite renombrar un equipo, validando que el nuevo nombre no esté vacío ni colisione con otro equipo distinto existente.
  * `eliminarEquipo(Long idEquipo)`: Desvincula de forma transaccional a todos los miembros del equipo y procede a la eliminación del equipo de la base de datos sin alterar ni borrar las cuentas de los usuarios.

### Capa de Controladores y Excepciones (`madstodolist.controller` y `madstodolist.controller.exception`)
* **`EquipoController`**:
  * `GET /equipos`: Listado general de equipos disponibles en el sistema.
  * `GET /equipos/{id}`: Detalle de un equipo específico, indicando sus miembros y renderizando dinámicamente las acciones permitidas.
  * `GET /equipos/nuevo` y `POST /equipos/nuevo`: Formulario y procesamiento para crear nuevos equipos.
  * `POST /equipos/{id}/unirse`: Endpoint para registrar al usuario autenticado en el equipo.
  * `POST /equipos/{id}/salir`: Endpoint para que el usuario autenticado abandone el equipo.
  * `GET /equipos/{id}/editar` y `POST /equipos/{id}/editar`: Formulario y acción de edición del nombre de un equipo, restringido exclusivamente al rol administrador.
  * `POST /equipos/{id}/eliminar`: Acción de borrado de un equipo, restringido exclusivamente al rol administrador.
  * Métodos de seguridad: `comprobarUsuarioLogeado()` (requiere sesión activa) y `comprobarAdmin()` (requiere rol de administrador activo, lanzando `UsuarioNoAutorizadoException` en caso contrario).
* **`EquipoNotFoundException`**: Mapeada a código HTTP 404 cuando se solicita un identificador de equipo que no existe.
* **`EquipoServiceException`**: Excepción de negocio para validaciones y reglas de dominio en operaciones con equipos.

---

## 3. Plantillas Thymeleaf Añadidas y Modificadas

1. **`fragments.html`**:
   * Barra de navegación común que incluye el acceso directo a la sección de **Equipos** (`/equipos`) para todos los usuarios autenticados.
2. **`listaEquipos.html`**:
   * Muestra la tabla de todos los equipos del sistema ordenados alfabéticamente.
   * Botón "Crear nuevo equipo" en la cabecera.
   * Enlaces a "Ver miembros" y, si el usuario autenticado es administrador (`th:if="${usuario.admin}"`), botones para **Editar** y **Eliminar** (con diálogo de confirmación JavaScript).
3. **`detalleEquipo.html`**:
   * Ficha de información del equipo con la tabla de sus participantes.
   * Botones contextuales:
     * Si no es miembro: botón verde "Unirse al equipo".
     * Si es miembro: botón rojo "Salir del equipo".
     * Si es administrador: botones adicionales para "Editar" y "Eliminar" el equipo.
4. **`formNuevoEquipo.html`**:
   * Formulario para dar de alta un equipo con control y renderizado de mensajes de error de validación.
5. **`formEditarEquipo.html`**:
   * Formulario para renombrar un equipo existente con precarga del nombre actual y visualización de posibles errores.

---

## 4. Tests Automatizados Implementados

La suite completa del proyecto cuenta con **114 tests automatizados**, todos ellos pasando satisfactoriamente:
* **Entidades y Repositorios (`EquipoTest`)**:
  * Creación y persistencia de `Equipo`.
  * Comprobación de unicidad de nombre en base de datos.
  * Sincronización de la relación bidireccional `@ManyToMany` entre `Equipo` y `Usuario`.
  * Verificación de que eliminar un equipo no borra a los usuarios asociados.
* **Servicios (`EquipoServiceTest`)**:
  * Creación y recuperación de equipos.
  * Listado ordenado alfabéticamente.
  * Validación de nombres vacíos o duplicados en creación y edición.
  * Incorporación y salida de usuarios de equipos.
  * Prevención de duplicados en la pertenencia y validación de usuarios inexistentes.
  * Consulta booleana de pertenencia (`usuarioEnEquipo`).
  * Desvinculación de usuarios al eliminar un equipo.
* **Controladores e Integración Web (`EquipoWebTest`)**:
  * Protección de rutas ante accesos anónimos (401 Unauthorized).
  * Protección de acciones administrativas (edición y borrado) ante usuarios sin rol de administrador (401 Unauthorized).
  * Renderizado condicional de botones según estado de pertenencia y rol de administrador.
  * Respuestas correctas ante creación, edición, borrado, unión y abandono de equipos.
  * Tratamiento de errores 404 ante equipos no existentes.

---

## 5. Fragmentos de Código Fuente Relevantes

### Desvinculación Transaccional al Eliminar un Equipo (`EquipoService.java`)
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

### Comprobación de Seguridad de Administrador en el Controlador (`EquipoController.java`)
```java
private UsuarioData comprobarAdmin() {
    UsuarioData usuario = comprobarUsuarioLogeado();
    if (!usuario.isAdmin()) {
        throw new UsuarioNoAutorizadoException();
    }
    return usuario;
}

@PostMapping("/equipos/{id}/eliminar")
public String eliminarEquipo(@PathVariable(value="id") Long idEquipo, RedirectAttributes flash) {
    comprobarAdmin();
    try {
        equipoService.eliminarEquipo(idEquipo);
        flash.addFlashAttribute("mensaje", "Equipo eliminado correctamente");
    } catch (EquipoServiceException e) {
        flash.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/equipos";
}
```
