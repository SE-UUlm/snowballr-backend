# Add auto-generated hint
TIMESTAMP=$(date -u +"%Y-%m-%d, %H:%M:%S UTC")
NOTICE="_This document was generated on $TIMESTAMP. Please do not edit this page directly._"

# Find markdown files recursively and add hint
find "./wiki" -type f -name "*.md" ! -name "_*" | while read -r file; do
  echo "Processing $file..."
  # Prepend text using temp file
  sed -i "1s/^/$NOTICE\n\n/" "$file"
done
