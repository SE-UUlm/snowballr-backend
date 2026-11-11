FROM markvincze/sabledocs:0.16.893-slim AS build

WORKDIR /build
COPY . .

WORKDIR /build/proto
RUN protoc ./*.proto -o ../descriptor.pb --include_source_info

WORKDIR /build
RUN pip3 install pypatch==1.0.2 markdown-mermaidjs==2.0.0 \
    && pypatch apply ./patches/sabledocs_fix_nested.patch sabledocs \
    && pypatch apply ./patches/sabledocs_nested_links.patch sabledocs \
    && pypatch apply ./patches/sabledocs_nested_names.patch sabledocs \
    && pypatch apply ./patches/sabledocs_fix_arguments.patch sabledocs \
    && pypatch apply ./patches/sabledocs_auth_tag.patch sabledocs \
    && pypatch apply ./patches/markdown_mermaidjs_no_script.patch markdown_mermaidjs \
    && sabledocs

FROM nginx AS deploy

COPY --from=build /build/sabledocs_output /usr/share/nginx/html
