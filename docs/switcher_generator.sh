#!/bin/bash

# Check if the user provided both the JSON and HTML output filenames as arguments
if [ -z "$1" ] || [ -z "$2" ]; then
    echo "Usage: $0 <json_output_file> <html_output_file>"
    exit 1
fi

# Get the output file names from the arguments
json_output_file="$1"
html_output_file="$2"

# Get all Git tags and filter by the form vX.Y.Z (semantic versioning)
tags=$(git tag --sort=-v:refname | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$')

# Initialize an empty array to hold JSON objects
json_array=()

# Initialize the HTML content
html_content="<html>
<head>
  <meta http-equiv=\"refresh\" content=\"0; url=https://corese-stack.github.io/corese-core/{{ latest_version }}/\">
  <title>Documentation Versions</title>
</head>
<body>
  <h1>Documentation Versions</h1>
  <ul>"

# Initialize the first flag to identify the latest tag
is_first=true
latest_version=""

# Loop through each tag
for tag in $tags; do
    # Determine if this is the latest version
    if $is_first; then
        preferred="true"
        name="$tag (latest)"
        latest_version="$tag"
        is_first=false
    else
        preferred="false"
        name="$tag (stable)"
    fi

    # Create a JSON object for the tag
    json_object=$(cat <<EOF
{
    "name": "$name",
    "version": "stable",
    "url": "https://corese-stack.github.io/corese-core/$tag/",
    "preferred": $preferred
}
EOF
)
    # Add the JSON object to the array
    json_array+=("$json_object")

    # Add HTML list item for the version
    if [ "$preferred" == "true" ]; then
        html_content="$html_content
    <li><a href=\"https://corese-stack.github.io/corese-core/$tag/\">$tag (latest)</a></li>"
    else
        html_content="$html_content
    <li><a href=\"https://corese-stack.github.io/corese-core/$tag/\">$tag</a></li>"
    fi
done

# Close the HTML content
html_content="$html_content
  </ul>
  <p>If you are not redirected, click <a href=\"https://corese-stack.github.io/corese-core/$latest_version/\">here</a>.</p>
</body>
</html>"

# Join the JSON objects into a single array
json_output=$(printf ",\n%s" "${json_array[@]}")

# Write the JSON output to the provided file
echo -e "[\n${json_output:2}\n]" > "$json_output_file"

# Write the HTML output to the provided file
html_content=$(echo "$html_content" | sed "s/{{ latest_version }}/$latest_version/")
echo "$html_content" > "$html_output_file"

# Print confirmation messages
echo "JSON data has been written to $json_output_file"
echo "HTML landing page has been written to $html_output_file"
