# corese-core documentation

To install the dependencies to build the documentation :

``` shell
pip install -r docs/requirements.txt
```

To compile the corese-core documentation :

``` shell
sphinx-multiversion docs/source build/html -D 'exhale_args.containmentFolder=${sourcedir}/java_api' -v
```
