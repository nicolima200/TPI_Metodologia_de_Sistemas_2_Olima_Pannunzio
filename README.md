# Gestión de Usuarios y Credenciales — TPI Metodología de Sistemas II

Trabajo Práctico Integrador de Metodología de Sistemas II (UTN — Tecnicatura Universitaria en Programación a Distancia).

El objetivo del trabajo es analizar y mejorar la calidad interna de un sistema existente mediante diagnóstico de deuda técnica, refactoring seguro y code review, conservando su comportamiento externo.

## Origen del sistema base

Este repositorio parte del proyecto integrador de Programación II:

- Repositorio original: https://github.com/nicolima200/UTN-Programacion2-Integrador
- Commit de partida: `6beb09b`
- Autores del proyecto original: Nicolás Olima y Leonel Mercorelli
- El análisis, refactoring y documentación realizados para Metodología de Sistemas II son de autoría de **NICOLÁS OLIMA** y **NICOLÁS PANNUNZIO**.

Sobre esa versión se incorporaron los scripts SQL de creación de la base y de datos de prueba (que no estaban en el repositorio original), unificando los nombres de las tablas con los utilizados en el código.

## Tags del trabajo

- `estado-inicial`: versión base del sistema, sin refactorizar.
- `etapa-diagnostico`: estado diagnosticado (Etapa 3).
- `etapa-refactoring` y `estadorefactorizado`: resultado de la serie de refactor, sobre el merge del último pull request.
- `etapa-pruebas`: resultado de la verificación de comportamiento (Etapa 6).
- `etapa-docs`: documentación de cierre (Etapa 8).

## Descripción

Aplicación de consola en Java que permite gestionar usuarios y sus credenciales de acceso sobre una base de datos MySQL:

- Crear usuario
- Listar usuarios
- Buscar usuario por ID
- Actualizar usuario
- Eliminar usuario (baja lógica)

## Estructura del código

- `src/main`: interfaz de consola. `Main` compone las dependencias, `AppMenu` orquesta el menú y `ConsolaUI` encapsula la lectura y la escritura por consola.
- `src/service`: reglas de negocio (`UsuarioService`, `CredencialAccesoService`), con el parameter object del alta en `dto/` y las excepciones de dominio en `exceptions/`.
- `src/dao`: acceso a datos con JDBC, con el mapeo de filas extraído por DAO.
- `src/entities`: modelo del dominio (`Usuario`, `CredencialAcceso`).
- `src/config`: proveedor de conexiones (`ConnectionProvider` y su implementación `DbConnectionProvider`) y la clase de prueba de conexión.

## Documentación

La carpeta `docs/` reúne las decisiones y la evidencia del trabajo:

- `docs/plan-refactor.md`: plan de la serie de siete pull requests, con cortes, criterios de aceptación y checklists de revisión.
- `docs/etapa-4-trazabilidad.md`: trazabilidad de cada debilidad al principio o regla que la viola y al pull request que la resuelve.
- `docs/code-review.md`: checklist técnico y resultado de las siete revisiones cruzadas.
- `docs/verificacion-etapa6.md`: protocolo y evidencia de la verificación antes/después del comportamiento.

El informe completo del trabajo se entrega en PDF junto con el repositorio.

## Tecnologías

- Java 24 (mínimo Java 15)
- MySQL
- JDBC con MySQL Connector/J 9.4.0 (incluido en `Lib/`)

## Instalación y uso

1. Ejecutar los scripts de la carpeta `sql/` en este orden:
   1. `creacion_tablas.sql`
   2. `datos_prueba.sql`
2. Configurar la conexión a la base de datos:
   - Copiar `db.properties.example` como   `db.properties` en la raíz del proyecto y completar `db.url`, `db.user` y `db.password`. Este archivo está en `.gitignore` y no se sube al repositorio.
   - Como alternativa, definir las variables de entorno `DB_URL`, `DB_USER` y `DB_PASSWORD`, que tienen prioridad sobre el archivo.
3. Agregar `Lib/mysql-connector-j-9.4.0/mysql-connector-j-9.4.0.jar` como dependencia del proyecto.
4. Ejecutar `src/main/Main.java`.