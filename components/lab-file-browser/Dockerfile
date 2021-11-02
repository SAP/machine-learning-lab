# build environment
FROM node:16-alpine as build
WORKDIR /app
ENV PATH /app/node_modules/.bin:$PATH
COPY webapp/package.json ./
RUN yarn install
COPY webapp/ ./
RUN yarn build

# production environment
FROM nginx:stable-alpine
#Copy react build
COPY --from=build /app/build /usr/share/nginx/html
#Copy shell scripts
COPY docker/ /resources/
COPY default_env.txt /

# Add bash
RUN apk add --no-cache bash

EXPOSE 80
ENTRYPOINT ["/bin/bash"]
CMD ["/resources/entrypoint.sh"]
