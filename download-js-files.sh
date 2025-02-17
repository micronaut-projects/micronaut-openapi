#!/bin/bash

SWAGGER_UI_INPUT="https://cdn.jsdelivr.net/npm/swagger-ui/dist/swagger-ui-bundle.js"
SWAGGER_UI_OUTPUT="openapi/src/main/resources/templates/swagger-ui/res/swagger-ui-bundle.js"

curl -L -o "$SWAGGER_UI_OUTPUT" "$SWAGGER_UI_INPUT"

if [ $? -eq 0 ]; then
  echo "File downloaded and saved to $SWAGGER_UI_OUTPUT"
else
  echo "Failed to download the file"
  exit 1
fi

SWAGGER_UI_STANDALONE_INPUT="https://cdn.jsdelivr.net/npm/swagger-ui/dist/swagger-ui-standalone-preset.js"
SWAGGER_UI_STANDALONE_OUTPUT="openapi/src/main/resources/templates/swagger-ui/res/swagger-ui-standalone-preset.js"

curl -L -o "$SWAGGER_UI_STANDALONE_OUTPUT" "$SWAGGER_UI_STANDALONE_INPUT"

if [ $? -eq 0 ]; then
  echo "File downloaded and saved to $SWAGGER_UI_STANDALONE_OUTPUT"
else
  echo "Failed to download the file"
  exit 1
fi

SWAGGER_UI_CSS_INPUT="https://cdn.jsdelivr.net/npm/swagger-ui/dist/swagger-ui.css"
SWAGGER_UI_CSS_OUTPUT="openapi/src/main/resources/templates/swagger-ui/res/swagger-ui.css"

curl -L -o "$SWAGGER_UI_CSS_OUTPUT" "$SWAGGER_UI_CSS_INPUT"

if [ $? -eq 0 ]; then
  echo "File downloaded and saved to $SWAGGER_UI_CSS_OUTPUT"
else
  echo "Failed to download the file"
  exit 1
fi

OPENAPI_EXPLORER_INPUT="https://cdn.jsdelivr.net/npm/openapi-explorer/dist/browser/openapi-explorer.min.js"
OPENAPI_EXPLORER_OUTPUT="openapi/src/main/resources/templates/openapi-explorer/res/openapi-explorer.min.js"

curl -L -o "$OPENAPI_EXPLORER_OUTPUT" "$OPENAPI_EXPLORER_INPUT"

if [ $? -eq 0 ]; then
  echo "File downloaded and saved to $OPENAPI_EXPLORER_OUTPUT"
else
  echo "Failed to download the file"
  exit 1
fi

REDOC_INPUT="https://cdn.jsdelivr.net/npm/redoc/bundles/redoc.standalone.js"
REDOC_OUTPUT="openapi/src/main/resources/templates/redoc/res/redoc.standalone.js"

curl -L -o "$REDOC_OUTPUT" "$REDOC_INPUT"

if [ $? -eq 0 ]; then
  echo "File downloaded and saved to $REDOC_OUTPUT"
else
  echo "Failed to download the file"
  exit 1
fi

RAPIDOC_INPUT="https://cdn.jsdelivr.net/npm/rapidoc/dist/rapidoc-min.js"
RAPIDOC_OUTPUT="openapi/src/main/resources/templates/rapidoc/res/rapidoc-min.js"

curl -L -o "$RAPIDOC_OUTPUT" "$RAPIDOC_INPUT"

if [ $? -eq 0 ]; then
  echo "File downloaded and saved to $RAPIDOC_OUTPUT"
else
  echo "Failed to download the file"
  exit 1
fi

RAPIPDF_INPUT="https://cdn.jsdelivr.net/npm/rapipdf/dist/rapipdf-min.js"
RAPIPDF_OUTPUT="openapi/src/main/resources/templates/rapipdf/res/rapipdf-min.js"

curl -L -o "$RAPIPDF_OUTPUT" "$RAPIPDF_INPUT"

if [ $? -eq 0 ]; then
  echo "File downloaded and saved to $RAPIPDF_OUTPUT"
else
  echo "Failed to download the file"
  exit 1
fi

SCALAR_INPUT="https://cdn.jsdelivr.net/npm/@scalar/api-reference/dist/browser/standalone.js"
SCALAR_OUTPUT="openapi/src/main/resources/templates/scalar/res/standalone.js"

curl -L -o "$SCALAR_OUTPUT" "$SCALAR_INPUT"

if [ $? -eq 0 ]; then
  echo "File downloaded and saved to $SCALAR_OUTPUT"
else
  echo "Failed to download the file"
  exit 1
fi
