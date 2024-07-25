# PrivacyMe

PrivacyMe es una aplicación de Android que permite seleccionar archivos multimedia (imágenes y
videos) y eliminar sus metadatos, como la ubicación, la fecha y la información del dispositivo, para
proteger la privacidad del usuario.

## Características

- Selección de archivos multimedia directamente desde la galería.
- Eliminación de metadatos de imágenes y videos.
- Manejo de permisos de lectura y escritura en el almacenamiento externo.

## Requisitos

- Android 10 o superior.
- Permisos de lectura y escritura en el almacenamiento externo.

## Uso

Al abrir la aplicación, se solicitarán los permisos necesarios. Asegúrate de concederlos para que la
aplicación funcione correctamente.

La aplicación lanzará automáticamente el selector de multimedia. Selecciona la imagen o video que
deseas procesar.

Una vez seleccionado el archivo, la aplicación eliminará los metadatos y mostrará un mensaje de
confirmación.

## Estructura del Proyecto

MainActivity.java: La actividad principal que maneja la solicitud de permisos y el lanzamiento del
selector de multimedia.
MetadataRemover.java: Clase que se encarga de eliminar los metadatos de los archivos multimedia.
AndroidManifest.xml: Archivo de configuración de la aplicación, incluyendo los permisos necesarios.

## Permisos

La aplicación requiere los siguientes permisos:

- READ_MEDIA_IMAGES: Para leer imágenes desde el almacenamiento externo.
- READ_MEDIA_VIDEO: Para leer videos desde el almacenamiento externo.
- WRITE_EXTERNAL_STORAGE: Para escribir archivos modificados en el almacenamiento externo.
  A partir de Android 10, se utiliza MediaStore para manejar archivos multimedia.

## Problemas Conocidos

En algunas versiones de Android, puede haber problemas con la manipulación de permisos. Asegúrate de
conceder todos los permisos necesarios.
La eliminación de metadatos puede no ser compatible con todos los formatos de archivo.

## Contribuciones

Las contribuciones son bienvenidas. Por favor, abre un issue o envía un pull request para discutir
cualquier cambio que te gustaría realizar.

## Licencia

Este proyecto está licenciado bajo la Licencia MIT. Consulta el archivo LICENSE para obtener más
información.

## Contacto

Para cualquier consulta o soporte, por favor contacta a gadiel068@gmail.com