# Documentación Técnica - Práctica 2: ToDoList

**Autor:** Iván Lafont  
**Asignatura:** Metodologías Ágiles de Desarrollo de Software (MADS)  
**Versión:** 1.1.0  

---

## 1. Introducción y Objetivos

Este documento recoge el resumen técnico de la evolución de la aplicación **ToDoList** durante la versión 1.1.0. Se han completado todas las historias de usuario obligatorias y opcionales:
1. **Barra de menú**: Navegación unificada y responsiva en toda la aplicación.
2. **Listado de usuarios**: Consulta del directorio de usuarios registrados.
3. **Descripción de usuario**: Visualización detallada del perfil público de un usuario.
4. **Usuario Administrador (opcional)**: Registro de un único administrador en el sistema con redirección directa a la administración de usuarios.
5. **Protección de usuarios (opcional)**: Restricción del listado y de la descripción de usuarios exclusivamente al rol administrador.
6. **Bloqueo de usuarios (opcional)**: Capacidad para que el administrador bloquee o habilite el acceso a la plataforma a cualquier usuario.

> **Nota metodológica sobre el flujo de trabajo en GitHub:**  
> En las funcionalidades obligatorias de la versión 1.1.0 (Barra de menú, Listado de usuarios y Descripción de usuario) se ha seguido de forma estricta y demostrable el flujo de trabajo de desarrollo ágil en GitHub: apertura previa de Issues, creación de ramas de características independientes (`feature branch`), subida de commits y apertura y mezcla formal de los Pull Requests correspondientes en GitHub (`#2`, `#4`, `#6`, `#8`).  
> Para el desarrollo de las 3 partes opcionales (Usuario Administrador, Protección y Bloqueo de usuarios), con el objetivo de agilizar la implementación y por motivos de eficiencia de tiempo, el desarrollo de las ramas y los merges se gestionaron directamente en local mediante merges `--no-ff` conservando la trazabilidad de la historia de Git. Se hace constar explícitamente el conocimiento pleno del flujo de trabajo habitual mediante Pull Requests e Issues en la plataforma web de GitHub, tal y como queda reflejado y validado en todas las entregas obligatorias precedentes y en las prácticas posteriores.

---

## 2. Nuevas Clases y Métodos Implementados

### Capa de Modelo y DTOs (`madstodolist.model` y `madstodolist.dto`)
* **`Usuario` y `UsuarioData`**:
  * Incorporan los atributos booleanos `admin` y `bloqueado`, junto con sus respectivos getters (`isAdmin()`, `isBloqueado()`) y setters (`setAdmin(boolean)`, `setBloqueado(boolean)`).
* **`RegistroData`**:
  * Añade el campo booleano `admin` con sus métodos de acceso para capturar la opción de alta como administrador desde el formulario de registro.

### Capa de Repositorios y Servicios (`madstodolist.repository` y `madstodolist.service`)
* **`UsuarioRepository`**:
  * `boolean existsByAdminTrue()`: Permite comprobar de forma optimizada en base de datos si ya existe un usuario con rol de administrador.
* **`UsuarioService`**:
  * `public enum LoginStatus`: Extendido con el estado `USER_BLOCKED` para identificar intentos de inicio de sesión de cuentas suspendidas.
  * `public LoginStatus login(String eMail, String password)`: Valida credenciales y comprueba si la cuenta se encuentra bloqueada, devolviendo `USER_BLOCKED` en tal caso.
  * `public boolean hayAdmin()`: Determina si ya existe un administrador en el sistema.
  * `public UsuarioData registrar(UsuarioData usuario)`: Si se intenta registrar un usuario como administrador y ya existe uno en la base de datos, lanza `UsuarioServiceException`.
  * `public void bloquearUsuario(Long idUsuario)` y `public void desbloquearUsuario(Long idUsuario)`: Modifican el estado de bloqueo de la entidad de forma transaccional.

### Capa de Controladores y Excepciones (`madstodolist.controller` y `madstodolist.controller.exception`)
* **`LoginController`**:
  * `registroForm(Model model)`: Añade el atributo `hayAdmin` al modelo para mostrar el checkbox de administrador únicamente si no existe ninguno.
  * `registroSubmit(...)`: Mapea el valor de `admin` desde `RegistroData` a `UsuarioData` y previene inconsistencias si ya existe un administrador.
  * `loginSubmit(...)`: Redirige al administrador directamente a `/registrados`, o al usuario estándar a sus tareas (`/usuarios/{id}/tareas`). Maneja el estado `USER_BLOCKED` mostrando un mensaje de error explicativo en la vista.
* **`UsuarioController`**:
  * `private UsuarioData comprobarAdmin()`: Método auxiliar de seguridad que verifica que exista un usuario logeado y que tenga privilegios de administrador.
  * `GET /registrados` y `GET /registrados/{id}`: Invocan `comprobarAdmin()`. Si el usuario no tiene permisos, se deniega el acceso lanzando las excepciones correspondientes.
  * `POST /registrados/{id}/bloquear` y `POST /registrados/{id}/desbloquear`: Endpoints protegidos para cambiar el estado de acceso de los usuarios registrados y redirigir de vuelta al listado.
* **`UsuarioNoAutorizadoException`**: Anotada con `@ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason = "Usuario no autorizado")`, retornando el código HTTP 401 si un usuario sin rol de administrador intenta acceder a recursos protegidos.
* **`UsuarioNotFoundException`**: Retorna el código HTTP 404 ante consultas de identificadores inexistentes.

---

## 3. Plantillas Thymeleaf Añadidas y Modificadas

1. **`fragments.html`**:
   * Barra de navegación común (`navbar`) que incluye enlace a *Tareas* para usuarios autenticados, enlace directo a *Usuarios* (`/registrados`) si el usuario es administrador, y enlaces a *Login* y *Registro* para visitantes.
2. **`formRegistro.html`**:
   * Incluye condicionalmente (`th:if="${!hayAdmin}"`) el checkbox para solicitar el rol de administrador en el alta del usuario.
3. **`listaUsuarios.html`**:
   * Tabla con el listado de usuarios que muestra su estado (*Activo* o *Bloqueado*) y proporciona botones de acción para ver detalles y para *Bloquear* o *Habilitar* al usuario (omitiéndose el botón de bloqueo para el propio administrador logeado).
4. **`descripcionUsuario.html`**:
   * Ficha de datos personales protegida que muestra el perfil del usuario consultado sin revelar su contraseña.

---

## 4. Tests Automatizados Implementados

La cobertura de pruebas abarca 55 tests automatizados distribuidos en:
* **Entidades y Repositorios (`UsuarioTest`)**: Pruebas de persistencia y propiedades `admin` y `bloqueado`.
* **Servicios (`UsuarioServiceTest`)**: Registro único de administrador, prevención de duplicación de administrador, bloqueo/desbloqueo de usuarios y verificación de `LoginStatus.USER_BLOCKED`.
* **Controladores Web (`LoginWebTest`, `UsuarioWebTest`, `NavbarWebTest`)**:
  * Redirección diferenciada tras login (administrador a `/registrados`, usuario normal a sus tareas).
  * Renderizado condicional del checkbox de administrador en el registro.
  * Bloqueo de acceso no autenticado (401) y no administrador (401) en `/registrados` y `/registrados/{id}`.
  * Respuestas correctas (200) ante peticiones de administradores autenticados.
  * Ejecución de peticiones `POST` para bloquear y desbloquear cuentas.

---

## 5. Fragmentos de Código Fuente Relevantes

El siguiente método en `UsuarioController.java` muestra la comprobación centralizada de autorización de administradores:

```java
private UsuarioData comprobarAdmin() {
    Long idUsuarioLogeado = managerUserSession.usuarioLogeado();
    if (idUsuarioLogeado == null) {
        throw new UsuarioNoLogeadoException();
    }
    UsuarioData usuario = usuarioService.findById(idUsuarioLogeado);
    if (usuario == null || !usuario.isAdmin()) {
        throw new UsuarioNoAutorizadoException();
    }
    return usuario;
}
```

Asimismo, en `UsuarioService.java` se controla el bloqueo en la autenticación:

```java
@Transactional(readOnly = true)
public LoginStatus login(String eMail, String password) {
    Optional<Usuario> usuario = usuarioRepository.findByEmail(eMail);
    if (!usuario.isPresent()) {
        return LoginStatus.USER_NOT_FOUND;
    } else if (!usuario.get().getPassword().equals(password)) {
        return LoginStatus.ERROR_PASSWORD;
    } else if (usuario.get().isBloqueado()) {
        return LoginStatus.USER_BLOCKED;
    } else {
        return LoginStatus.LOGIN_OK;
    }
}
```
