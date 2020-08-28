# Modify webpack config, otherwise the Swagger API client lib cannot be loaded (also stated in their Readme file)
# Do it in this file during build, because on fresh installation the webpack file is cleaned.
echo "Preparing webpack config"
# just add the parser-field somewhere in the config. In the string-to-be-replaced add '$' so it is only replaced once (when you build the webapp multiple times)
replaceStringParserAmd='s/include: paths.appSrc,$/include: paths.appSrc, parser: {amd: false},/g'

# set 'useEslintrc' to true, so we can define global variables in package.json. 
# Otherwise, the build command dumps because of the Swagger-generated files' variable 'define', which is not defined.
replaceStringEsLintrc='s/useEslintrc: isExtendingEslintConfig,/useEslintrc: true,/g'

# set 'skipWaiting: true' so that the ServiceWorker is automatically activated and the newest version
# is served, instead a cached version. Otherwise, a ServiceWorker is only updated in case all tabs where the
# app was running are closed before re-opening the page.
addSkipWaitingToServiceWorker='s/clientsClaim: true,$/clientsClaim: true, skipWaiting: true,/g'

nodeEndpointInjection="process.env.REACT_APP_LAB_ENDPOINT"
remoteBackendPath='"http://localhost:32900/api"'
replaceEndpoint="s@$remoteBackendPath@$nodeEndpointInjection@g"
if [ "$1" = "localstartbuild" ]; then
  replaceEndpoint="s@$nodeEndpointInjection@$remoteBackendPath@g"
elif [ "$1" = "localendbuild" ]; then
  cp -R build/ app/; mv app/ build/app
fi

webpackPath=./node_modules/react-scripts/config/webpack.config.js #.*.js

constantsFilePath=./src/services/handler/constants.js
if [ $(uname) = 'Darwin' ]
then
  sed -i '' "$replaceStringParserAmd" $webpackPath
  sed -i '' "$replaceStringEsLintrc" $webpackPath
  sed -i '' "$addSkipWaitingToServiceWorker" $webpackPath

  sed -i '' "$replaceEndpoint" $constantsFilePath
elif [ $(uname) = 'Linux' ]
then  
  sed -i "$replaceStringParserAmd" $webpackPath
  sed -i "$replaceStringEsLintrc" $webpackPath
  sed -i "$addSkipWaitingToServiceWorker" $webpackPath

  sed -i "$replaceEndpoint" $constantsFilePath
fi
