# SnowballR API

This directory contains the API specification of the _SnowballR_ project.
For communication between the backend and frontend, [gRPC](https://grpc.io) is
used, whereas the services are defined by Protocol Buffers
(see [/proto](./proto/) for the service definition
(in [main.proto](./proto/main.proto)) and all required `.proto` files).

The API documentation is generated from the `.proto` files with
[sabledocs](https://github.com/markvincze/sabledocs) (templates in [/docs](./docs/), patches in
[/patches](./patches/)) as a stage of the backend `Dockerfile`, and served by the backend under `/grpc-docs/`.
