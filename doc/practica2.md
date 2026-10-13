# Documentación Técnica - Práctica 2: ToDoList

**Autor:** Iván Lafont  
**Asignatura:** Metodologías Ágiles de Desarrollo de Software (MADS)  
**Versión:** 1.1.0  

---

## 1. Introducción y Objetivos

Este documento recoge el resumen técnico de la evolución de la aplicación **ToDoList** durante la versión 1.1.0. El objetivo principal ha sido ampliar la funcionalidad de la plataforma permitiendo la navegación unificada en toda la aplicación, la consulta y visualización del directorio de usuarios registrados y la exposición detallada del perfil de cada usuario, manteniendo una arquitectura limpia desacoplada por capas (controlador, servicio, DTO y repositorio) y garantizando la cobertura de pruebas automatizadas en cada componente.

---

## 2. Nuevas Clases y Métodos Implementados

Para estructurar las nuevas funcionalidades respetando el patrón de diseño establecido en Spring Boot, se han introducido y modificado las siguientes clases y métodos:

### Capa de Controladores (`madstodolist.controller`)
* **`UsuarioController`**:
  * `public String listadoUsuarios(Model model)`: Gestiona las peticiones `GET /registrados`. Si el usuario tiene sesión activa, recupera sus datos a través de `ManagerUserSession` para añadirlos al modelo y personalizar la barra de navegación; a continuación, obtiene la lista completa de usuarios registrados y delega la representación en la plantilla `listaUsuarios`.
  * `public String descripcionUsuario(@PathVariable("id") Long idUsuario, Model model)`: Atiende las peticiones `GET /registrados/{id}`. Obtiene los datos del usuario identificado por `idUsuario`. En caso de no existir, lanza la excepción personalizada `UsuarioNotFoundException`. Si existe, inyecta el objeto DTO `usuarioConsultado` en el modelo y devuelve la vista `descripcionUsuario`.

### Capa de Excepciones (`madstodolist.controller.exception`)
* **`UsuarioNotFoundException`**: Excepción de tipo `RuntimeException` anotada con `@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Usuario no encontrado")`. Permite que Spring devuelva de forma transparente una respuesta HTTP 404 cuando se solicita un identificador de usuario inexistente sin propagar errores no controlados.

### Capa de Servicios y DTOs (`madstodolist.service` y `madstodolist.dto`)
* **`UsuarioService`**:
  * `public List<UsuarioData> findAll()`: Consulta a través de `UsuarioRepository` la totalidad de las entidades de usuarios registradas en la base de datos y utiliza `ModelMapper` para convertirlas a una colección de objetos `UsuarioData`, ocultando detalles internos de la entidad persistente.
  * `public UsuarioData findById(Long id)`: Busca un usuario por su identificador único y lo mapea a `UsuarioData` si está presente.
* **`UsuarioData`**: Incluye los campos `id`, `nombre`, `email` y `fechaNacimiento`, permitiendo transferir únicamente la información requerida hacia la vista sin exponer contraseñas ni claves de acceso.

---

## 3. Plantillas Thymeleaf Añadidas y Modificadas

1. **`fragments.html`**:
   * Se ha definido e integrado el fragmento `navbar`. Este componente genera una barra de navegación superior utilizando Bootstrap, visible en todas las páginas autenticadas y en la página informativa. Muestra enlaces a *ToDoList* (`/about`), *Tareas* (`/usuarios/{id}/tareas`) y un menú desplegable con el nombre del usuario y la opción de cerrar sesión (`/logout`). En caso de no haber sesión iniciada (como al visitar `/about` sin autenticar), presenta enlaces directos a *Login* y *Registro*.
2. **`listaUsuarios.html`**:
   * Presenta una tabla con la relación de usuarios del sistema (identificador y correo electrónico) e incorpora para cada fila un botón de acción (*Detalles*) que enlaza directamente a la vista individual `/registrados/{id}`.
3. **`descripcionUsuario.html`**:
   * Muestra mediante un componente de tarjeta (*Card*) los detalles del usuario consultado: identificador, nombre completo, correo electrónico y fecha de nacimiento formateada (`dd/MM/yyyy`). Añade un botón para regresar a la lista de registrados.

---

## 4. Tests Automatizados Implementados

Se han implementado pruebas en dos niveles distintos para asegurar la robustez de las soluciones:

* **Tests de Capa de Servicio (`UsuarioServiceTest`)**:
  * Comprobación del método `findAll()`: Verifica que tras insertar múltiples usuarios en la base de datos, el servicio devuelve la lista completa mapeada correctamente a DTOs.
  * Comprobación de `findById()`: Verifica tanto la recuperación satisfactoria de un usuario por su ID como la respuesta nula ante identificadores que no existen.
* **Tests de Capa Web y Controladores (`UsuarioWebTest` y `NavbarWebTest`)**:
  * `listadoUsuariosDevuelveLista()`: Simula peticiones `GET /registrados` con `MockMvc`, mockeando el servicio para asegurar que se retorna el código de estado 200 y que los correos electrónicos se renderizan en el HTML resultante.
  * `descripcionUsuarioDevuelveDatosUsuario()`: Comprueba que al consultar `/registrados/1` se muestra el nombre y el correo, y verifica explícitamente que la contraseña **no** aparece reflejada en el contenido devuelto.
  * `descripcionUsuarioDevuelveNotFoundSiNoExiste()`: Verifica que ante un usuario no registrado se obtiene un código HTTP 404 Not Found.
  * Pruebas del Navbar: Validan la presencia de los enlaces correspondientes según el estado de la sesión del usuario.

---

## 5. Fragmentos de Código Fuente Relevantes

El siguiente fragmento de `UsuarioController.java` ilustra el control de acceso a los datos de detalle de un usuario, la integración con la sesión y la gestión limpia del error 404:

```java
@GetMapping("/registrados/{id}")
public String descripcionUsuario(@PathVariable(value="id") Long idUsuario, Model model) {
    Long idUsuarioLogeado = managerUserSession.usuarioLogeado();
    if (idUsuarioLogeado != null) {
        UsuarioData usuarioLogeado = usuarioService.findById(idUsuarioLogeado);
        model.addAttribute("usuario", usuarioLogeado);
    }

    UsuarioData usuarioConsultado = usuarioService.findById(idUsuario);
    if (usuarioConsultado == null) {
        throw new UsuarioNotFoundException();
    }

    model.addAttribute("usuarioConsultado", usuarioConsultado);
    return "descripcionUsuario";
}
```

La comprobación y lanzamiento de `UsuarioNotFoundException` permite responder con semántica REST estándar delegando en la anotación `@ResponseStatus` de Spring MVC, evitando lógica adicional de redirección o mensajes de error ad-hoc.
