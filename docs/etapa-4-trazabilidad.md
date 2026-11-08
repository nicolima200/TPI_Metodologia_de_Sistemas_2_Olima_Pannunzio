# Etapa 4 — Trazabilidad de SOLID y Clean Code

Este documento conecta cada debilidad del inventario de la Etapa 3 (S1 a S11) con el principio o regla que la viola, el PR que la resuelve y la evidencia del informe. Los PRs son los mismos de `plan-refactor.md`: la Etapa 4 justifica por qué cambia cada cosa (el principio) y la Etapa 5 registra cómo (la técnica de refactoring con nombre propio). El diff no se duplica.

En GitHub, los siete pull requests están numerados del #1 al #7 en el mismo orden de la tabla de trazabilidad: #1 = 1a, #2 = 1b, #3 = 2a, #4 = 2b, #5 = 3a, #6 = 3b y #7 = 4. La serie completa se mergeó en `main` el 7 de noviembre de 2026; las revisiones y veredictos quedaron registrados en `code-review.md`.

## Trazabilidad PR → principio

| PR | Rama | Debilidad | Principio / regla | Cambio que lo aplica | Archivos | Commits |
|----|------|-----------|-------------------|----------------------|----------|---------|
| 1a | `refactor/consola-ui` | S1 | SRP | Extraer `ConsolaUI` e inyectarla por constructor | `ConsolaUI.java` (nuevo), `AppMenu.java`, `Main.java` | 2 |
| 1b | `refactor/consola-ui-retorno` | S2 | Clean Code | `this.mostrarMenu()` pasa a `return`; se aplanan los `else` | `AppMenu.java` | 1 |
| 2a | `fix/dao-columna-credencial` | S5 | Corrección de defecto | `id` pasa a `id_CredencialAcceso` en `leerTodos` | `CredencialAccesoDao.java` | 1 |
| 2b | `refactor/dao-mapeo` | S6 | DRY | Extraer `mapearUsuario` y `mapearCredencial`, constante `COLUMNAS`, try-with-resources | `UsuarioDao.java`, `CredencialAccesoDao.java` | 1 |
| 3a | `refactor/servicio-alta` | S7, S11 | SRP + Clean Code | `validarUsername`/`validarEmail` (`void`, lanzan excepción) y record `AltaUsuario` | `AltaUsuario.java` (nuevo), `UsuarioService.java`, `AppMenu.java` | 2 |
| 3b | `refactor/servicios` | S3, S4, S8 | SRP + manejo de errores | Transacción única (`actualizarConCredencial`, `eliminarConCredencial`, `revertir`), credencial por `getCredencial().getId()`, `PersistenciaException`, `AppMenu` sin `java.sql` | `PersistenciaException.java` (nuevo), `UsuarioService.java`, `CredencialAccesoService.java`, `GenericService.java`, `AppMenu.java` | 3 |
| 4 | `refactor/dip-conexion` | S9 | DIP | `ConnectionProvider` y `DbConnectionProvider`; inyección desde `Main`; se elimina `DatabaseConnection` | `ConnectionProvider.java`, `DbConnectionProvider.java` (nuevos), `DatabaseConnection.java` (eliminado), servicios, `Main.java`, `TestConexion.java` | 2 |
| — | — | S10 | Clean Code | Parcial: los validadores se renombran en 3a; el resto queda registrado como pendiente | — | — |

S11 se resuelve en su parte alcanzable (el parameter object `AltaUsuario` en 3a); los value objects `Email` y `Username` quedan como mejora futura.

## Evidencia por PR

La evidencia de la Etapa 4 son fragmentos de código antes/después y tablas. Las capturas de ejecución de los casos E pertenecen a la Etapa 6; las capturas del diff de GitHub, a la Etapa 7 (conversación del PR, revisión y merge). El código se tipografía, no se fotografía.

| PR | Fragmento antes/después | Tabla | Captura |
|----|-------------------------|-------|---------|
| 1a | `buscarUsuario` con `System.out`/`leerLong()` frente a `ui.leerId(PROMPT_ID_BUSCAR)` | Interfaz de `ConsolaUI` (6 métodos) | Build verde |
| 1b | Rama del id 0: `this.mostrarMenu()` frente a `return`, con el `else` aplanado | — | — |
| 2b | `mapearUsuario` antes (duplicado en `leer` y `leerTodos`) frente al método extraído | Las cuatro consultas y su constante `COLUMNAS` | — |
| 3a | `usernameValido` `boolean` frente a `validarUsername` `void`; parámetros sueltos frente al record | Campos de `AltaUsuario` | Build verde |
| 3b | `catch SQLException` frente a `PersistenciaException`; `eliminarConCredencial` con `commit`/`revertir` | Caminos de cada transacción | — |
| 4 | `DatabaseConnection.getConnection()` estático frente al proveedor inyectado (`Main`) | Quién provee la conexión: antes/después | — |

## Cobertura de principios

| Principio | Dónde se aplica | Estado |
|-----------|-----------------|--------|
| SRP | 1a (entrada/salida), 3a (validación y construcción de entidades), 3b (transacción y errores fuera de la vista) | Aplicado |
| DIP | 4 | Aplicado |
| OCP | 1a y 1b bajan el costo de agregar operaciones, no lo eliminan | Documentado como mejora futura |
| LSP | No hay herencia de clases; ninguna subclase reemplaza a otra | No aplica |
| ISP | `GenericService` obliga a implementar `getAll()` en credenciales aunque ningún flujo lo use | Violación menor, documentada |

## Notas

- El PR 2a es una corrección de defecto, no una mejora de SOLID ni de Clean Code. Se informa como habilitador de la cadena de DAOs (2b reescribe el archivo y perdería el arreglo) y se referencia desde la Etapa 3 como corrección del smell S5.
- La Etapa 4 y la Etapa 5 usan los mismos PRs. En el informe, la sección de SOLID toma el principio y la de refactorings la técnica con nombre propio: Extract Method (1a, 2b), Move Method y reubicación de responsabilidad (3b, 4), Introduce Parameter Object (3a).
- El informe carga la sección completa de la Etapa 4 en `TPI_MDS_II_Olima_Pannunzio.docx` (secciones 4.1 a 4.9), con los fragmentos y las tres tablas.
- Tags de la consigna: `estado-inicial`, `etapa-diagnostico` (sobre 2a73ce1, el estado diagnosticado), `etapa-refactoring` y `estadorefactorizado` (sobre 69dbde1, el merge del PR #7) ya están creados. Faltan `etapa-pruebas` y `etapa-docs`, que se crean al cerrar las etapas 6 y 8.
