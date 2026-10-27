# Guía Completa de Estudio y Defensa - Práctica 3: MADS

**Autor:** Iván Lafont  
**Asignatura:** Metodologías Ágiles de Desarrollo de Software (MADS)  
**Versión del proyecto:** 1.2.0  

---

## Índice de Contenidos
1. [Visión Global de la Práctica 3](#1-visión-global-de-la-práctica-3)
2. [Integración Continua con GitHub Actions](#2-integración-continua-con-github-actions)
3. [Configuración de PostgreSQL, Perfiles y Docker](#3-configuración-de-postgresql-perfiles-y-docker)
4. [Fundamentos de TDD (Test-Driven Development)](#4-fundamentos-de-tdd-test-driven-development)
5. [Desglose Técnico de las Historias de Usuario](#5-desglose-técnico-de-las-historias-de-usuario)
   * [Historia 008: Listado de equipos](#historia-008-listado-de-equipos)
   * [Historia 009: Gestionar pertenencia al equipo](#historia-009-gestionar-pertenencia-al-equipo)
   * [Historia 010: Gestión de equipos (administrador)](#historia-010-gestión-de-equipos-administrador)
6. [Conceptos Clave de Arquitectura y Patrones Utilizados](#6-conceptos-clave-de-arquitectura-y-patrones-utilizados)
7. [Batería de Preguntas Típicas del Profesor y Cómo Responderlas](#7-batería-de-preguntas-típicas-del-profesor-y-cómo-responderlas)

---

## 1. Visión Global de la Práctica 3

El objetivo principal de esta práctica es dominar dos pilares fundamentales de la ingeniería ágil de software moderna:
1. **Integración Continua (CI)** mediante **GitHub Actions** ejecutando pruebas automáticas contra dos entornos: base de datos en memoria (H2) y base de datos real en contenedor (PostgreSQL).
2. **Desarrollo Guiado por Pruebas (TDD - Test-Driven Development)** para diseñar e implementar desde dentro hacia afuera (modelo $\rightarrow$ repositorio $\rightarrow$ servicio $\rightarrow$ controlador/vista) una nueva funcionalidad compleja: **la gestión de equipos y miembros**.

### Hitos de la entrega
* **Versión / Release:** `v1.2.0` (marcada con tag de Git en GitHub).
* **Número total de pruebas:** **114 tests automatizados**, todos pasando al 100%.
* **Flujo en GitHub:** Uso estricto de GitHub Projects / Issues, ramas de características (`feature branches`), commits modulares y Pull Requests mezclados en `main`.

---

## 2. Integración Continua con GitHub Actions

### ¿Qué es la Integración Continua (CI)?
Es la práctica ágil de fusionar las copias de trabajo de todos los desarrolladores en una rama compartida (`main`) con frecuencia (varias veces al día). Cada integración se verifica automáticamente mediante una construcción (*build*) y la ejecución de la suite de tests automatizados para detectar errores lo antes posible.

### Los dos flujos de trabajo (*workflows*) configurados
En el directorio `.github/workflows/` disponemos de dos ficheros YAML:

#### A) `developer-tests.yml` (Developer Tests)
* **Objetivo:** Ejecutar rápidamente los tests usando la base de datos H2 en memoria.
* **Disparador:** `on: push` (en cualquier commit o PR).
* **Entorno:** Contenedor `openjdk:8-jdk-alpine` sobre máquina virtual Ubuntu.
* **Comando ejecutado:** `./mvnw test`.

#### B) `integration-tests.yml` (Integration Tests)
* **Objetivo:** Ejecutar las pruebas contra un motor de base de datos relacional idéntico al de producción (**PostgreSQL 13**).
* **Mecanismo:** Utiliza la directiva `services:` de GitHub Actions para levantar un contenedor Docker enlazado con la imagen `postgres:13`.
* **Variables de entorno:** Configura el usuario `POSTGRES_USER=mads`, la contraseña `POSTGRES_PASSWORD=mads` y la base de datos `POSTGRES_DB=mads_test`.
* **Salud del contenedor:** Usa `--health-cmd pg_isready` para no lanzar Maven hasta que la base de datos esté lista para aceptar conexiones.
* **Comando ejecutado:** `./mvnw test -D spring.profiles.active=postgres`, inyectando `POSTGRES_HOST: postgres`.

> **¿Por qué mantener ambos workflows?**  
> Los tests de desarrollador (H2) son ultrarrápidos y permiten retroalimentación instantánea. Los tests de integración garantizan que las peculiaridades de SQL, tipos de datos y dialectos de PostgreSQL no rompan nada antes de hacer el merge en `main`.

---

## 3. Configuración de PostgreSQL, Perfiles y Docker

### El sistema de perfiles de Spring Boot
Spring Boot permite definir ficheros de configuración según el entorno:
1. `src/main/resources/application.properties`: Configuración base (perfil por defecto: `dev` con base de datos H2 en memoria).
2. `src/main/resources/application-postgres.properties`: Perfil de **ejecución local** con PostgreSQL (base de datos `mads` en `localhost:5432`).
3. `src/test/resources/application-postgres.properties`: Perfil de **test** con PostgreSQL (base de datos `mads_test`).

### ¿Por qué se usan dos bases de datos distintas (`mads` y `mads_test`)?
Para que la ejecución de la suite de pruebas (que limpia y borra datos antes de cada test con `clean-db.sql` y `hibernate.ddl-auto=create`) **no destruya los datos reales** que el desarrollador ha creado manualmente mientras probaba la aplicación en desarrollo.

### Variables parametrizadas en el perfil de test
En `src/test/resources/application-postgres.properties`:
```properties
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
DB_USER=mads
DB_PASSWD=mads
spring.datasource.url=jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/mads_test
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWD}
spring.jpa.properties.hibernate.dialect = org.hibernate.dialect.PostgreSQL9Dialect
```
Esto permite que en local funcione contra `localhost`, pero en GitHub Actions se pueda sobreescribir con `-D POSTGRES_HOST=postgres` sin tocar el código.

---

## 4. Fundamentos de TDD (Test-Driven Development)

### La regla de oro de TDD
> **No se escribe ni una sola línea de código de producción sin que antes exista un test automatizado que haya fallado.**

### Ciclo Red - Green - Refactor
1. **Red (Rojo):** Escribir una prueba unitaria pequeña que especifique el siguiente comportamiento requerido. Compilar/ejecutar y ver cómo **falla** (o no compila).
2. **Green (Verde):** Escribir el **código mínimo estrictamente necesario** para que la prueba pase. No adelantarse ni añadir código extra.
3. **Refactor (Refactorizar):** Limpiar el código, eliminar duplicidades, mejorar nombres o modularizar, manteniendo todos los tests en verde.
4. **Commit:** Registrar el incremento funcional en Git.

### Commits modulares (~15–25 líneas)
En lugar de hacer commits gigantescos de 300 líneas, la práctica exige modularidad: un commit por cada test superado. Esto proporciona:
* Trazabilidad paso a paso del razonamiento.
* Facilidad para hacer `revert` o aislar fallos si un commit introduce una regresión.
* Demostración empírica de que se ha seguido TDD.

---

## 5. Desglose Técnico de las Historias de Usuario

---

### Historia 008: Listado de equipos
**Objetivo:** Permitir a los usuarios consultar los equipos existentes ordenados alfabéticamente y ver los miembros de cada equipo.

#### La batería guiada de 10 Tests TDD (Capa Modelo y Servicio):
1. **Primer test (`crearEquipo` en `EquipoTest`):** Construcción básica del POJO `Equipo` con constructor y getter de `nombre`.
2. **Segundo test (`grabarYBuscarEquipo` en `EquipoTest`):** Mapeo JPA con `@Entity`, `@Table(name = "equipos")`, clave `@Id @GeneratedValue` y guardado en `EquipoRepository`. Inclusión de `DELETE FROM equipos` en `clean-db.sql`.
3. **Tercer test (`comprobarIgualdadEquipos` en `EquipoTest`):** Implementación de `equals` y `hashCode`. Si no tienen ID asignado se compara por `nombre`; si tienen ID, se compara por `id`.
4. **Cuarto test (`comprobarRelacionBaseDatos` en `EquipoTest`):** Relación `@ManyToMany` bidireccional entre `Equipo` y `Usuario`.
   * En `Equipo`: `@ManyToMany(fetch = FetchType.LAZY)` con `@JoinTable(name = "equipo_usuario", joinColumns = @JoinColumn(name = "fk_equipo"), inverseJoinColumns = @JoinColumn(name = "fk_usuario"))`.
   * En `Usuario`: `@ManyToMany(mappedBy = "usuarios") Set<Equipo> equipos`.
   * Método de sincronización: `equipo.addUsuario(usuario)` actualiza ambas colecciones.
5. **Quinto test (`comprobarFindAll` en `EquipoTest`):** Declarar `List<Equipo> findAll()` en la interfaz `EquipoRepository`.
6. **Sexto test (`crearRecuperarEquipo` en `EquipoServiceTest`):** Capa de servicio (`EquipoService`) devolviendo DTOs (`EquipoData`) mapeados con `ModelMapper`.
7. **Séptimo test (`listadoEquiposOrdenAlfabetico` en `EquipoServiceTest`):** Implementación de `findAllOrdenadoPorNombre()` utilizando `Comparator.comparing(Equipo::getNombre)`.
8. **Octavo test (`añadirUsuarioAEquipo` en `EquipoServiceTest`):** Métodos de servicio `añadirUsuarioAEquipo(idEquipo, idUsuario)` y `usuariosEquipo(idEquipo)`.
9. **Noveno test (`recuperarEquiposDeUsuario` en `EquipoServiceTest`):** Método `equiposUsuario(idUsuario)` para consultar la relación inversa desde el servicio.
10. **Décimo test (`comprobarExcepciones` en `EquipoServiceTest`):** Verificación de que si el equipo o usuario no existen en la BD, se lanza `EquipoServiceException`.

#### Capa de Vista y Controller (Rama `vista-equipos`):
* Enlace **Equipos** en el menú de navegación (`fragments.html`).
* Controlador `EquipoController`:
  * `GET /equipos`: Renderiza `listaEquipos.html` con la lista ordenada.
  * `GET /equipos/{id}`: Renderiza `detalleEquipo.html` con los participantes.
* Tests en `EquipoWebTest` verificando seguridad (requiere login) y renderizado de HTML.

---

### Historia 009: Gestionar pertenencia al equipo
**Objetivo:** Permitir crear nuevos equipos y que los usuarios puedan unirse o salir de cualquier equipo.

#### La batería de 10 Tests TDD (Rama `servicio-pertenencia-equipos`):
1. **Test 1:** Método `removeUsuario(Usuario)` en la entidad `Equipo` desvinculando en memoria en ambos sentidos.
2. **Test 2:** Persistencia JPA de la desvinculación: al llamar a `removeUsuario`, la fila correspondiente en la tabla intermedia `equipo_usuario` se elimina físicamente.
3. **Test 3:** Servicio `eliminarUsuarioDeEquipo(idEquipo, idUsuario)`.
4. **Test 4:** Validación: excepción si se intenta eliminar a un usuario que no pertenece al equipo.
5. **Test 5:** Validación: excepción al intentar eliminar de un equipo o usuario inexistente.
6. **Test 6:** Validación al crear equipo: excepción si el nombre es nulo, vacío (`""`) o solo espacios (`"   "`).
7. **Test 7:** Validación al crear equipo: excepción si ya existe un equipo con ese mismo nombre (no duplicados).
8. **Test 8:** Validación al añadir usuario: excepción si el equipo o usuario no existen en la BD.
9. **Test 9:** Validación al añadir usuario: excepción si el usuario ya es miembro de ese equipo.
10. **Test 10:** Consulta de pertenencia `usuarioEnEquipo(idEquipo, idUsuario)` devolviendo `boolean` (`true` tras unirse, `false` tras salir).

#### Capa de Vista y Controller (Rama `vista-pertenencia-equipos`):
* `GET /equipos/nuevo` y `POST /equipos/nuevo`: Muestra y procesa `formNuevoEquipo.html` gestionando errores de validación.
* `POST /equipos/{id}/unirse`: Añade al usuario en sesión al equipo y redirige con mensaje flash.
* `POST /equipos/{id}/salir`: Da de baja al usuario en sesión del equipo y redirige con mensaje flash.
* `detalleEquipo.html`: Comprueba la variable booleana `esMiembro`:
  * Si `!esMiembro`: Muestra botón verde **"Unirse al equipo"**.
  * Si `esMiembro`: Muestra botón rojo **"Salir del equipo"**.
* Tests exhaustivos en `EquipoWebTest`.

---

### Historia 010: Gestión de equipos (administrador)
**Objetivo:** Permitir únicamente a los administradores modificar el nombre de un equipo y eliminar equipos sin borrar sus usuarios asociados.

#### La batería de 10 Tests TDD (Rama `servicio-gestion-equipos`):
1. **Test 1:** Modificar nombre en la entidad `Equipo` (`equipo.setNombre(...)`).
2. **Test 2:** Servicio `modificarEquipo(idEquipo, nuevoNombre)` y persistencia.
3. **Test 3:** Excepción si se intenta modificar un equipo que no existe.
4. **Test 4:** Validación: excepción si el nuevo nombre es nulo o vacío.
5. **Test 5:** Validación: excepción si el nuevo nombre ya está ocupado por otro equipo.
6. **Test 6:** Permitir guardar si el nombre introducido es exactamente el mismo que ya tenía el equipo.
7. **Test 7:** A nivel de entidad y BD: eliminar un equipo no borra a los usuarios asociados.
8. **Test 8:** Servicio `eliminarEquipo(idEquipo)`.
9. **Test 9:** Desvinculación limpia en el servicio: antes de eliminar el equipo, se desvinculan todos sus miembros para limpiar la tabla de unión `equipo_usuario` sin tocar la tabla `usuarios`.
10. **Test 10:** Excepción al intentar eliminar un equipo que no existe.

#### Capa de Vista y Controller (Rama `vista-gestion-equipos`):
* Seguridad en `EquipoController`:
  ```java
  private UsuarioData comprobarAdmin() {
      UsuarioData usuario = comprobarUsuarioLogeado();
      if (!usuario.isAdmin()) {
          throw new UsuarioNoAutorizadoException(); // HTTP 401
      }
      return usuario;
  }
  ```
* `GET /equipos/{id}/editar` y `POST /equipos/{id}/editar`: Formulario `formEditarEquipo.html` accesible solo por administradores.
* `POST /equipos/{id}/eliminar`: Acción protegida de borrado.
* Vistas adaptativas: En `listaEquipos.html` y `detalleEquipo.html`, los botones de **"Editar"** y **"Eliminar"** tienen la directiva `th:if="${usuario.admin}"`. Si el usuario no es admin, ni siquiera se renderizan en el DOM.

---

## 6. Conceptos Clave de Arquitectura y Patrones Utilizados

### 1. Patrón DTO (Data Transfer Object) y ModelMapper
* **Problema:** Enviar entidades JPA (`Equipo`, `Usuario`) directamente a la vista (Thymeleaf) o al controlador provoca problemas graves:
  * Exposición de campos sensibles (como contraseñas).
  * Excepciones `LazyInitializationException` si la sesión de Hibernate se cierra antes de que Thymeleaf renderice colecciones lazy.
  * Modificaciones accidentales en la base de datos por *dirty checking* de Hibernate.
* **Solución:** Los servicios devuelven siempre `EquipoData` y `UsuarioData`. Se usa `ModelMapper` para convertir entre la entidad interna y el DTO externo.

### 2. Transaccionalidad (`@Transactional`)
* Se declara en todos los métodos de `EquipoService`.
* En métodos de consulta se usa `@Transactional(readOnly = true)`: optimiza el rendimiento porque Hibernate desactiva el control de cambios en memoria (*dirty checking*) y libera recursos más rápido.
* En operaciones complejas (como `eliminarEquipo`), `@Transactional` garantiza la propiedad de **Atomicidad**: o se desvinculan los usuarios y se borra el equipo, o si algo falla se hace un *rollback* completo sin dejar la base de datos en estado inconsistente.

### 3. Sincronización Bidireccional en relaciones `@ManyToMany`
En JPA, cuando una relación es bidireccional, Hibernate solo persiste lo que ocurre en el lado **dueño** (*owning side*, el que tiene `@JoinTable`, en nuestro caso `Equipo`). Sin embargo, en memoria de la JVM, si no actualizas la colección inversa (`usuario.getEquipos().add(this)`), el objeto `Usuario` tendrá datos obsoletos hasta que se refresque de la BD.  
Por eso se implementaron métodos de conveniencia:
```java
public void addUsuario(Usuario usuario) {
    this.getUsuarios().add(usuario);
    usuario.getEquipos().add(this);
}

public void removeUsuario(Usuario usuario) {
    this.getUsuarios().remove(usuario);
    usuario.getEquipos().remove(this);
}
```

---

## 7. Batería de Preguntas Típicas del Profesor y Cómo Responderlas

### P1: "¿Por qué en los tests de servicio no pusisteis `@Transactional` pero en los de repositorio sí?"
> **Respuesta ideal:**  
> "En los tests de repositorio usamos `@Transactional` para que la conexión y la sesión de Hibernate permanezcan abiertas durante todo el test y se haga rollback al terminar. Sin embargo, en `EquipoServiceTest` no debemos poner `@Transactional` en la clase de test porque queremos probar los métodos de servicio en el mismo entorno real en el que los llamará el controlador (sin transacción abierta por defecto). De este modo nos aseguramos de que cada método del servicio gestiona su propia transacción con su `@Transactional` y que no se accede a proxies perezosos (Lazy) fuera de la capa de servicio."

### P2: "¿Por qué los controladores no tienen lógica de negocio ni consultas directas al repositorio?"
> **Respuesta ideal:**  
> "Por el principio de separación de responsabilidades y la arquitectura en capas. Los controladores solo deben encargarse del protocolo HTTP: recibir peticiones, comprobar autenticación/sesión, llamar al método de servicio correspondiente, capturar posibles excepciones de negocio (`EquipoServiceException`) y pasar datos al modelo para la vista. Toda la lógica de negocio y validación debe residir en el servicio para que sea reutilizable y esté 100% testeada mediante TDD."

### P3: "¿Cómo conseguisteis que al borrar un equipo no se borrasen los usuarios?"
> **Respuesta ideal:**  
> "En primer lugar, la relación `@ManyToMany` no tiene configurado `CascadeType.REMOVE` ni `CascadeType.ALL`. En segundo lugar, antes de invocar `equipoRepository.delete(equipo)`, en el método `eliminarEquipo` del servicio recorremos una copia de la colección de usuarios (`new HashSet<>(equipo.getUsuarios())`) y ejecutamos `equipo.removeUsuario(usuario)`. Esto elimina las filas correspondientes en la tabla intermedia `equipo_usuario` sin tocar la tabla `usuarios`."

### P4: "¿Por qué se usa `new HashSet<>(equipo.getUsuarios())` al desvincular en lugar de iterar directamente sobre `equipo.getUsuarios()`?"
> **Respuesta ideal:**  
> "Porque si iteramos directamente sobre `equipo.getUsuarios()` y dentro del bucle llamamos a `equipo.removeUsuario(usuario)`, estaríamos modificando la misma colección sobre la que estamos iterando, lo que lanzaría una excepción `ConcurrentModificationException`. Al crear un `new HashSet<>()` iteramos sobre una copia y modificamos la colección original de forma segura."

### P5: "¿Cómo garantizáis que un usuario no administrador no pueda borrar un equipo enviando un POST con Postman o cURL?"
> **Respuesta ideal:**  
> "No nos fiamos únicamente de ocultar los botones en el HTML. En el backend, el método `eliminarEquipo` del controlador ejecuta primero `comprobarAdmin()`. Si no hay sesión activa o el usuario no tiene la propiedad `admin == true`, se lanza inmediatamente `UsuarioNoAutorizadoException`, que devuelve un código de estado HTTP 401 Unauthorized, impidiendo la ejecución de la acción aunque se envíe la petición directamente."

### P6: "¿Cómo se prueba que una rama de GitHub Actions falla si rompemos un test?"
> **Respuesta ideal:**  
> "En la fase 2 de la práctica hicimos exactamente esa prueba: alteramos un test intencionadamente para que fallase, hicimos push y comprobamos en la pestaña *Actions* de GitHub que el build se marcaba con una 'X' roja y el Pull Request bloqueaba el merge hasta corregir el fallo."
