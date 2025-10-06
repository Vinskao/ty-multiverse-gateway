#!/bin/bash

echo "🧪 Testing People gRPC endpoint..."
echo ""
echo "📮 Sending POST request to: http://localhost:8082/tymgateway/tymb/people/get-by-name"
echo "📦 Body: {\"name\":\"Maya\"}"
echo ""

curl -X POST http://localhost:8082/tymgateway/tymb/people/get-by-name \
  -H "Content-Type: application/json" \
  -d '{"name":"Maya"}' \
  | python3 -m json.tool 2>/dev/null || cat

echo ""

