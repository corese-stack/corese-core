# corese-core documentation

The documentation is based on a combination of Sphinx parsing Exhale rst output genrated from Doxygen xml output.

## Dependencies

It requires installing some dependencies, installation that can be leverage using pip.

To install the dependencies to build the documentation:

``` shell
pip install -r docs/requirements.txt

```

## Documentation generation

Following that, the corese-core documentation can be generated through a single call to sphinx-multiversion from the root directory of corese-core:

``` shell
sphinx-multiversion docs/source build/html -D 'exhale_args.containmentFolder=${sourcedir}/java_api' -v
```

## Switcher generation

- To navigate between versions by means of the switcher (the dropdown list indicating the available version), the switcher.json object must be generated.
- To improve navigability, a landing page must also be generated to redirect to the latest version of the documentation.

To this end a script must be executed and write the output to the output html directory:

```shell
./docs/switcher_generator.sh build/html/switcher.json build/html/index.html
```

Both sphinx-multiversion and switcher_generator work on tags following the ```^v[0-9]+\.[0-9]+\.[0-9]+$``` syntax and ordered by refname.

## Alternatives

Alternatively, one may want to build the raw doxygen documentation:

- Update the Doxyfile to set GENERATE_HTML variable to YES
- Run doxygen in the docs/source directory
