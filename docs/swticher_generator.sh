#!/bin/bash

# Get all Git tags
tags=$(git tag --sort=-v:refname | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$')

# Initialize an empty array to hold JSON objects
json_array=()

# Initialize the first flag to identify the latest tag
is_first=true

# Loop through each tag
for tag in $tags; do
    # Determine if this is the latest version
    if $is_first; then
        preferred="true"
        name="$tag (latest)"
        is_first=false
    else
        preferred="false"
        name="$tag (stable)"
    fi

    # Create a JSON object for the tag
    json_object=$(cat <<EOF
{
    "Name": "$name",
    "version": "stable",
    "url": "https://corese-stack.github.io/corese-core/$tag/",
    "preferred": $preferred
}
EOF
)
    # Add the JSON object to the array
    json_array+=("$json_object")
done

# Join the JSON objects into a single array
json_output=$(printf ",\n%s" "${json_array[@]}")

# Wrap the JSON array with square brackets
echo -e "[\n${json_output:2}\n]"
