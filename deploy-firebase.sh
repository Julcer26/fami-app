#!/usr/bin/env bash
set -euo pipefail

# Despliegue completo de reglas RTDB, Storage, Cloud Functions (incluyendo proxy Gemini) y Hosting para Fami.
# Requisitos:
#   1. Tener Firebase CLI instalado: npm i -g firebase-tools
#   2. Haber iniciado sesión: firebase login
#   3. Configurar la API Key de Gemini como secreto en Firebase Functions (una sola vez):
#        firebase functions:secrets:set GEMINI_API_KEY

PROJECT_ID="hogar-finanzas-2026"

echo "Installing Cloud Functions dependencies..."
npm --prefix functions ci --omit=dev || npm --prefix functions install --omit=dev

echo "Validating database.rules.json and functions/index.js..."
python3 -c "import json; json.load(open('database.rules.json')); json.load(open('firebase.json'))"
node -c functions/index.js

echo "Deploying Database Rules, Storage Rules, Cloud Functions and Hosting to ${PROJECT_ID}..."
firebase use "${PROJECT_ID}"
firebase deploy --only database,storage,functions,hosting

echo "Firebase deploy completed successfully."
