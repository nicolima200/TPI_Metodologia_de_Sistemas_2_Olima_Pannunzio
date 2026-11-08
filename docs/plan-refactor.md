# Plan de refactor: etapas 4 y 5

## Objetivo

Aplicar SRP, DIP y reglas de Clean Code sobre `AppMenu`, los servicios y los DAO, sin cambiar el comportamiento externo. Cada cambio resuelve un smell identificado en la Etapa 3 y queda en un PR revisable, con un veredicto de revisión propio. Cada PR se aplica sobre la rama en pasos con nombre propio que compilan por separado.

## Alcance

Dentro: `src/main`, `src/service`, `src/dao`, `src/config`.
Fuera: entidades, esquema SQL, scripts de datos.

Cada PR se prueba a mano con los casos E que le corresponden antes de pedir revisión. Las pruebas automatizadas llegan en la Etapa 6.

## Reglas de trabajo

- Una rama por PR, con prefijo `refactor/` o `fix/`.
- Cada rama sale de `main` con los PR anteriores de su cadena ya mergeados.
- Commits atómicos, en formato Conventional Commits y en español. Cada commit compila por sí solo y resuelve un paso con nombre propio. El PR se revisa como unidad, no commit por commit.
- La división en commits no cambia el alcance ni el tamaño del PR. Los archivos tocados y el corte son los mismos, y la medición de líneas se hace sobre la rama completa.
- Cada PR lo revisa la otra persona. Nadie mergea su propio PR.
- El merge se hace con merge commit, nunca squash: el historial del PR conserva los pasos.
- El PR se abre con los criterios de aceptación y el checklist del corte en el cuerpo.
- Si GitHub informa conflictos, la rama se actualiza con `git pull --rebase origin main` y se publica con `git push --force-with-lease`. Sin conflictos no hace falta rebasear.
- Un PR no supera las 300 líneas cambiadas, contando agregados más borrados. La medición se hace con `git diff --stat` sobre la rama, no sobre un archivo suelto.
- Mensajes de la aplicación sin cambios: los textos de los casos E son el contrato.

## Cortes planificados

La serie se corta en siete pull requests. Cada uno abre cuando su base ya está mergeada, de modo que el diff de revisión no arrastra los cortes anteriores.

| Corte | Rama | Autor | Revisor | Base |
|----|------|-------|---------|------|
| 1a | `refactor/consola-ui` | Olima | Pannunzio | `main` |
| 1b | `refactor/consola-ui-retorno` | Olima | Pannunzio | `main` con 1a mergeado |
| 2a | `fix/dao-columna-credencial` | Pannunzio | Olima | `main` |
| 2b | `refactor/dao-mapeo` | Pannunzio | Olima | `main` con 2a mergeado |
| 3a | `refactor/servicio-alta` | Pannunzio | Olima | `main` con 1b y 2b mergeados |
| 3b | `refactor/servicios` | Pannunzio | Olima | `main` con 3a mergeado |
| 4 | `refactor/dip-conexion` | Olima | Pannunzio | `main` con 3b mergeado |

El historial de cada rama queda en commits atómicos: un paso con nombre propio y build en verde por commit, en formato Conventional Commits y en español. La cantidad la marca el trabajo de cada corte; lo que el plan fija es la granularidad, no el número.

Los PR 1a y 1b, de un lado, y 2a y 2b, del otro, no comparten archivos: las dos cadenas avanzan en paralelo. La cadena de interfaz sigue con 3a, 3b y 4; la de persistencia termina en 2b.

El límite de este plan gobierna el precio de cada corte: ningún PR supera las 300 líneas cambiadas, contando agregados más borrados, y el tamaño de la rama se comprueba con `git diff --stat` antes de abrir el PR. Como parte del código se mueve de archivo entre cortes consecutivos, la suma de los diffs contra `main` no refleja el trabajo de cada rama; la medición válida es la de la rama contra su base.

## Criterio de corte

Un corte se acepta solo si se cumplen las cuatro condiciones juntas:

1. Cada PR deja el build en verde por sí solo.
2. Cada PR responde una pregunta de revisión distinta y se aprueba o se rechaza sin depender del siguiente.
3. Ningún archivo se reescribe dos veces en PRs consecutivos: si dos cortes seguidos tocan el mismo archivo, el segundo cambia zonas distintas y su diff no repite el primero.
4. No es un PR de trámite sin veredicto propio; en la práctica, menos de unas 40 líneas no alcanza para sostener una revisión.

La condición 3 explica la forma de las cadenas: 1b, 2b y 3b vuelven sobre un archivo que ya tocó el PR anterior, pero solo sobre las zonas que ese PR dejó pendientes. 1b cambia el control de flujo del menú; 2b, el mapeo y las consultas de los DAO; 3b, la parte transaccional y de errores de los servicios.

### Por qué no se parte más

- **1a no se divide en salida y entrada.** Los dos pases reescribirían cada método del menú: uno las impresiones y el otro las lecturas. El segundo diff reescribiría casi todo el primero y ninguno de los dos tendría un veredicto completo. La lectura de ids acompaña al resto de la lectura: sin ella, `AppMenu` no puede dejar de conocer `Scanner` porque sus lecturas internas siguen acopladas al flujo.
- **3a no separa los validadores del parámetro del alta en dos PRs.** Sería un renombre sin criterio de aceptación propio: cae en la condición 4. Comparten el mismo flujo de revisión, que es el alta. Dentro del PR, el renombre y el parameter object van como pasos atómicos separados: son pasos internos, no unidades de revisión.
- **3b no se separa en dos PRs.** Separar transacciones y error propio obliga a reescribir `UsuarioService` dos veces seguidas y a dejar una rama intermedia donde el servicio ya no declara la excepción de persistencia pero todavía no tiene una de dominio que la reemplace. Cae en la condición 3. Dentro del PR, la migración del error y la atomicidad sí van como pasos separados: son pasos internos, no unidades de revisión.
- **4 no se separa en dos PRs.** Un PR intermedio con el proveedor nuevo y el viejo conviviendo duplica el adaptador sobre la clase de conexión existente y su código de configuración, que el paso siguiente vuelve a borrar. El intermedio no tiene veredicto propio y rompe la condición 3. Dentro del PR, primero llega el proveedor y después la inyección con la eliminación de la clase vieja.

## PR 1a: extraer la consola de AppMenu

Autor: Olima. Revisor: Pannunzio.

### Archivos

- Nuevo: `src/main/ConsolaUI.java`
- Modificado: `src/main/AppMenu.java`
- Modificado: `src/main/Main.java`

### Diseño previsto

El menú hoy mezcla tres responsabilidades: mostrar y leer consola, operar sobre usuarios y credenciales, y orquestar el flujo. Este PR separa la primera. Se crea `ConsolaUI`, que encapsula el `Scanner` y los flujos `System.out` y `System.err`, y expone las operaciones de escritura, lectura y pausa que usa el menú, conservando los tiempos actuales. `AppMenu` recibe la consola por constructor y canaliza toda su E/S por ella: deja de conocer `Scanner`, los flujos de salida y sus helpers privados de lectura. `Main` construye `ConsolaUI` y la inyecta.

La recursión del retorno al menú queda intacta en este PR: se resuelve aparte en 1b, con su propio criterio de aceptación.

### Casos a probar

- E1 y E2 con los mismos textos.
- E3 con los mismos resultados.
- Cancelar con 0 en buscar, actualizar y eliminar: aparece "Volviendo...", corre la pausa de 2 segundos y se vuelve al menú.

### Criterios de aceptación

- Los textos y el orden de los mensajes no cambian.
- `AppMenu` no contiene `System.out`, `System.err` ni `Scanner`.
- La clase compila con `javac`.

### Checklist de revisión

- [ ] Ningún mensaje cambió de texto ni de orden.
- [ ] Las pausas de 2 segundos siguen existiendo donde estaban.
- [ ] `ConsolaUI` no conoce entidades ni servicios.
- [ ] No quedan métodos de lectura duplicados en `AppMenu`.
- [ ] La recursión se conservó tal cual, para tratarla en un PR aparte.

## PR 1b: volver al menú sin recursión

Autor: Olima. Revisor: Pannunzio. Base: `main` con 1a mergeado.

### Archivos

- Modificado: `src/main/AppMenu.java`

### Diseño previsto

Es un diff pequeño y de control de flujo: en las tres operaciones del menú, la rama de cancelación con id 0 deja de reutilizar la recursión de `mostrarMenu()`, vuelve directamente y vuelve a recorrer el menú una sola vez. Se aplanan los `else` que cuelgan de esa rama. Nada más; el riesgo del corte está en tocar de más.

### Casos a probar

- Cancelar con 0 en las tres operaciones no apila menús: se vuelve una sola vez y la opción 0 cierra el programa.
- E3 se comporta igual que antes del cambio.

### Criterios de aceptación

- `mostrarMenu()` no se llama desde ningún método de operación.
- Los mensajes no cambian.
- `AppMenu` sigue compilando sin advertencias nuevas.

### Checklist de revisión

- [ ] No se tocó nada fuera de los tres métodos.
- [ ] No quedaron llaves ni ramas muertas después de aplanar.
- [ ] La salida del programa ocurre en un solo ciclo del menú.

## PR 2a: corregir la columna de la credencial

Autor: Pannunzio. Revisor: Olima.

### Archivos

- Modificado: `src/dao/CredencialAccesoDao.java`

### Diseño previsto

Es el defecto S5 de la Etapa 3: el listado de credenciales filtra por una columna que no existe en la tabla según el esquema de `sql/creacion_tablas.sql`, y la consulta lanza `SQLException` al recorrer el resultado. El PR corrige el nombre de columna en la consulta defectuosa y no toca nada más.

### Casos a probar

- `credencialService.getAll()` devuelve la lista sin lanzar excepción.
- E9 con los mismos valores.

### Criterios de aceptación

- La consulta del listado recorre el `ResultSet` sin excepción.
- El resto de los métodos del DAO queda igual.

### Checklist de revisión

- [ ] El nombre de la columna coincide con el esquema de `sql/creacion_tablas.sql`.
- [ ] El diff se limita a la consulta defectuosa.
- [ ] No se agregaron imports ni se reformateó el archivo.

## PR 2b: extraer el mapeo y nombrar columnas

Autor: Pannunzio. Revisor: Olima. Base: `main` con 2a mergeado.

### Archivos

- Modificado: `src/dao/UsuarioDao.java`
- Modificado: `src/dao/CredencialAccesoDao.java`

### Diseño previsto

Cada DAO duplica el mapeo de filas a entidades en dos métodos de lectura (S6). Se extrae un método privado de mapeo por DAO, compartido por las lecturas. Las consultas dejan de usar `SELECT *`: cada una nombra sus columnas, de modo que el código declara qué lee. Los `ResultSet` de las lecturas se cerrarán explícitamente y no quedará ninguno colgado del ciclo de vida del statement. Ninguna operación pública cambia de firma ni desaparece. La corrección de 2a se conserva: el mapeo de credenciales lee la columna real del esquema.

### Casos a probar

- E3, E4, E5 y E9 devuelven los mismos valores que antes del cambio.
- `credencialService.getAll()` sigue sin excepción después de la reescritura.

### Criterios de aceptación

- Las cuatro consultas siguen filtrando `eliminado = false` donde corresponde.
- El mapeo no cambia tipos ni valores.
- El DAO no expone métodos nuevos.

### Checklist de revisión

- [ ] El nombre de cada columna coincide con el esquema de `sql/creacion_tablas.sql`. MySQL no distingue mayúsculas en nombres de columna, así que `id_credencialAcceso` e `id_CredencialAcceso` refieren a la misma.
- [ ] El mapeo no cambia tipos ni valores.
- [ ] No se tocó el SQL de creación ni los datos de prueba.
- [ ] El arreglo de 2a sigue presente.

## PR 3a: parameter object del alta y validación por excepción

Autor: Pannunzio. Revisor: Olima. Base: `main` con 1b y 2b mergeados.

### Archivos

- Nuevo: `src/service/dto/AltaUsuario.java`
- Modificado: `src/service/UsuarioService.java`
- Modificado: `src/main/AppMenu.java`

### Diseño previsto

Dos objetivos en el mismo flujo de revisión, el alta:

- **Parameter object (S3).** La vista arma el par de entidades del alta a mano y las pasa al servicio. Se introduce un parámetro único con los cuatro datos que aporta la pantalla (usuario, email, hash y salt); el servicio arma las entidades a partir de él con los valores por defecto vigentes. Con eso `AppMenu` deja de construir entidades.
- **Validación con excepción de dominio.** Los validadores devuelven boolean y confían en que el llamador actúe (S7). Pasan a lanzar la excepción de validación ellos mismos, con los mismos mensajes, y el servicio la valida antes de tocar la base.

En este PR no se tocan transacciones, ni el error de persistencia, ni la forma en que el servicio obtiene la conexión: eso es 3b y 4.

### Casos a probar

- E1: alta de un usuario con datos válidos.
- E2: alta con username o email inválido, con el mismo mensaje de validación que antes.

### Criterios de aceptación

- La validación sigue ocurriendo antes de abrir la conexión.
- Los textos de error de validación no cambian.
- `AppMenu` ya no construye `Usuario` ni `CredencialAcceso` para el alta.

### Checklist de revisión

- [ ] El parámetro del alta tiene los cuatro campos que pide la pantalla, ni uno más.
- [ ] Los validadores lanzan la excepción y conservan los mensajes.
- [ ] No se filtró ningún cambio de transacción en este PR.
- [ ] `crearUsuario` no quedó con código muerto.

## PR 3b: transacciones atómicas y error de persistencia

Autor: Pannunzio. Revisor: Olima. Base: `main` con 3a mergeado.

### Archivos

- Nuevo: `src/service/exceptions/PersistenciaException.java`
- Modificado: `src/service/UsuarioService.java`
- Modificado: `src/service/CredencialAccesoService.java`
- Modificado: `src/service/GenericService.java`
- Modificado: `src/main/AppMenu.java`

### Diseño previsto

- **Atomicidad (S4).** La actualización y la baja tocan dos tablas con la misma conexión estática y sin transacción: si la segunda escritura falla, la primera queda confirmada. Los servicios pasan a abrir una sola conexión y una sola transacción por operación, confirmando en el camino feliz y revirtiendo en el de error. El rollback centraliza el manejo del error de reversión sin tapar la causa original.
- **Cierre duplicado (S8).** El alta cierra la conexión dos veces. Los recursos se administran con try-with-resources.
- **Error de dominio.** Los servicios capturan la excepción de persistencia y lanzan una propia, y la interfaz del servicio deja de declarar la excepción de JDBC en toda su superficie. El menú captura solo el error de dominio: deja de importar `java.sql` y deja de operar credenciales, porque actualizar y eliminar son ahora operaciones atómicas del servicio de usuarios.

### Casos a probar

- E1 a E9 completos.
- Actualizar un usuario actualiza su credencial y ninguna otra.
- Forzar un error en la segunda escritura, por ejemplo con un username duplicado: la primera no queda confirmada.
- El sistema arranca sin excepciones sin capturar.

### Criterios de aceptación

- E2 sigue mostrando el mismo mensaje de validación.
- `AppMenu` no menciona `Connection`, `SQLException` ni DAO.
- Cada transacción tiene commit en el camino feliz y rollback en el de error.
- El error de rollback no reemplaza a la causa original.

### Checklist de revisión

- [ ] Ningún `catch` vacío.
- [ ] Ninguna conexión se cierra dos veces.
- [ ] El rollback está en todos los caminos de error.
- [ ] Las operaciones públicas nuevas describen la operación.
- [ ] `AppMenu` no importa `java.sql`.

## PR 4: invertir la dependencia de la conexión

Autor: Olima. Revisor: Pannunzio. Base: `main` con 3b mergeado.

### Archivos

- Nuevo: `src/config/ConnectionProvider.java`
- Nuevo: `src/config/DbConnectionProvider.java`
- Eliminado: `src/config/DatabaseConnection.java`
- Modificado: `src/service/UsuarioService.java`
- Modificado: `src/service/CredencialAccesoService.java`
- Modificado: `src/main/AppMenu.java`
- Modificado: `src/main/Main.java`
- Modificado: `src/config/TestConexion.java`

### Diseño previsto

La conexión la provee una clase estática (S9): los servicios dependen de una utilería concreta y nadie más puede decidir la implementación (DIP). Se introduce una interfaz de proveedor de conexiones y su implementación, que toma el código de configuración existente (variables de entorno, `db.properties` y `-Ddb.config`) con el mismo orden de prioridad y el mismo mensaje de configuración incompleta; la diferencia es que el estado deja de ser estático. Los servicios reciben el proveedor por constructor. `Main`, como composición de arranque, arma el proveedor y los servicios y se los pasa a `AppMenu`: es el único lugar que decide qué implementación se usa. La clase estática se elimina y `TestConexion` pasa a usar el proveedor, para que ninguna referencia sobreviva.

### Casos a probar

- E1 a E9 completos.
- Arranque con `db.properties` presente y con variables de entorno.
- Sin configuración, el mensaje de error es el mismo de antes.

### Criterios de aceptación

- Ningún archivo menciona la clase de conexión eliminada.
- Ningún servicio decide la implementación de la conexión.
- El comportamiento de configuración no cambia: mismo orden de prioridad y mismo mensaje.

### Checklist de revisión

- [ ] El proveedor no conoce entidades ni servicios.
- [ ] No quedan llamadas estáticas a la conexión.
- [ ] `Main` es el único lugar que instancia la implementación del proveedor.
- [ ] El archivo eliminado no tiene referencias.

## Principios que no se aplican

- LSP: no hay herencia de clases. Ninguna subclase reemplaza a otra.
- ISP: `GenericService` obliga a implementar `getAll()` en credenciales aunque ningún flujo lo use. Queda documentado como violación menor: partir la interfaz agrega ruido sin beneficio en este alcance.
- OCP: agregar una operación nueva sigue obligando a tocar `AppMenu`. Los PR 1a y 1b bajan el costo porque cada operación es un método y el menú solo despacha, pero no lo eliminan. Queda como mejora futura.

## Orden de integración

1. Tag `etapa-diagnostico` sobre `main` antes de empezar.
2. Abrir 1a y 2a desde `main`, en paralelo. Revisión cruzada: Pannunzio revisa 1a, Olima revisa 2a.
3. Merge de 1a. Abrir 1b desde `main` actualizado. Merge de 2a y abrir 2b del mismo modo.
4. Merge de 1b y de 2b. El orden entre ambas cadenas no altera el resultado.
5. Abrir 3a desde `main` con 1b y 2b mergeados. Revisión de Olima y merge.
6. Abrir 3b desde `main` con 3a mergeado. Revisión de Olima y merge.
7. Abrir 4 desde `main` con 3b mergeado. Revisión de Pannunzio y merge.
8. Tag `etapa-refactoring` sobre el merge del PR 4.

## Conflictos previstos

- `AppMenu` es el archivo más tocado: 1a cambia la E/S, 1b el control de flujo, 3a el alta, 3b la actualización y la baja, y 4 el constructor. Como los PR están serializados sobre `main`, no hay conflicto textual entre ellos; cualquier edición paralela de `AppMenu` sí lo produce.
- `UsuarioService` lo tocan 3a, 3b y 4. 3a agrega el parámetro del alta y cambia la validación; 3b reescribe la parte transaccional; 4 cambia la forma de obtener la conexión. Por eso van en ese orden.
- `CredencialAccesoDao` lo tocan 2a y 2b. 2a corrige el nombre de columna y 2b reescribe el archivo; 2b debe salir después del merge de 2a para no perder el arreglo.
- Los PR 2a y 2b no tocan `AppMenu` ni los servicios, así que esa cadena puede mergearse en cualquier momento respecto de la cadena de interfaz.
- Si 3a se escribe antes del merge de 1b, el diff de `AppMenu` se resuelve dos veces: una contra la recursión y otra contra el alta.

## Tags

Los tags de la consigna marcan los hitos del trabajo:

- `estado-inicial`: el código de partida.
- `etapa-diagnostico`: el estado diagnosticado en la Etapa 3.
- `etapa-refactoring` y `estadorefactorizado`: el merge del PR 4.
- `etapa-pruebas` y `etapa-docs`: se crean al cerrar las etapas 6 y 8.

## Verificación local

Antes de pedir revisión, cada corte se verifica en tres pasos:

- El proyecto compila completo, desde la raíz, con el classpath del conector de MySQL.
- El tamaño de la rama, medido con `git diff --stat` contra su base, queda por debajo de 300 líneas entre agregados y borrados.
- El menú se ejecuta con la base creada y se recorren los casos E asignados al corte.

## Riesgos

- Cambiar firmas de servicio obliga a tocar `AppMenu`. Si 3a o 3b se escriben antes de 1b, el conflicto se resuelve dos veces.
- El PR 1a es el corte más grande de la serie: reescribe el menú completo y agrega la consola nueva. No admite trabajo extra; si aparece más superficie de UI, corresponde a otro PR.
- El proyecto no tiene pruebas automatizadas todavía. Toda verificación es manual hasta la Etapa 6.
- Regenerar los datos de prueba puede cambiar los ids y ocultar problemas de la relación 1 a 1. Usar `sql/datos_prueba.sql` sin modificar.
- Si alguien toca `entidades` o el esquema, el alcance declarado en la Etapa 1 deja de cumplirse.
