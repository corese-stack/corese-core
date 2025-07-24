.. CORESE documentation master file, created by
   sphinx-quickstart on Tue Apr 16 14:51:03 2024.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.


.. image:: _static/logo/corese-core_doc_light.svg
   :align: center
   :width: 400px
   :class: only-light

.. image:: _static/logo/corese-core_doc_dark.svg
   :align: center
   :width: 400px
   :class: only-dark


Corese is a software platform that implements and extends the standards of the Semantic Web. It enables users to create, manipulate, parse, serialize, query, reason about, and validate RDF data.

Corese-Core is a Java library that provides the core functionalities of Corese. It allows developers to build applications that process RDF data and programmatically leverage Corese's features via its API.

.. Define named hyperlinks for the references of W3C standards
.. _RDF: https://www.w3.org/RDF/
.. _RDFS: https://www.w3.org/2001/sw/wiki/RDFS
.. _SPARQL1.1 Query & Update: https://www.w3.org/2001/sw/wiki/SPARQL
.. _OWL RL: https://www.w3.org/2005/rules/wiki/OWLRL
.. _SHACL: https://www.w3.org/TR/shacl/

.. Define named hyperlinks for the references of extensions
.. _STTL SPARQL: ./_static/extensions/sttl.html
.. _SPARQL Rule: ./_static/extensions/rule.html
.. _LDScript: ./_static/extensions/ldscript.html

.. Original location of the extensions documentation
.. .. _STTL SPARQL: https://files.inria.fr/corese/doc/sttl.html
.. .. _SPARQL Rule: https://files.inria.fr/corese/doc/rule.html
.. .. _LDScript: https://files.inria.fr/corese/doc/ldscript.html


.. #############################################################################
.. The statements below are to produce the grid of cards in the home page
.. grid:: 2

    .. grid-item-card::  
      :shadow: sm
      :class-card: sd-rounded-3

      Corese implements W3C standards and extensions
      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
      * W3C standards
         * `RDF`_
         * `RDFS`_
         * `SPARQL1.1 Query & Update`_
         * `OWL RL`_
         * `SHACL`_
      * Extensions
         * `STTL SPARQL`_
         * `SPARQL Rule`_
         * `LDScript`_

    .. grid-item-card::
      :shadow: sm
      :class-card: sd-rounded-3
   
      Corese offers several interfaces
      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
      * `corese-core <https://corese-stack.github.io/corese-core/>`_: Java library to process RDF data and use Corese features via an API.
      * `corese-command <https://corese-stack.github.io/corese-command/>`_: Command Line Interface for Corese that allows users to interact with Corese features from the terminal.
      * `corese-gui <https://corese-stack.github.io/corese-gui-swing/>`_: Graphical interface that allows an easy and visual use of Corese features.
      * `corese-server <https://corese-stack.github.io/corese-server-jetty/>`_: Tool to create, configure and manage SPARQL endpoints.
      * `corese-python (beta) <https://corese-stack.github.io/corese-python/>`_: Python wrapper for accessing and manipulating RDF data with Corese features using py4j.

.. raw:: html

   <h3>Contributions and discussions about Corese-Core</h3>

.. _discussion forum: https://github.com/orgs/corese-stack/discussions
.. _issue reports: https://github.com/corese-stack/corese-core/issues
.. _pull requests: https://github.com/corese-stack/corese-core/pulls

For support questions, comments, and any ideas for improvements you`d like to discuss, please use our `discussion forum`_. We welcome everyone to contribute to `issue reports`_, suggest new features, and create `pull requests`_.


.. #############################################################################
.. The statements below are to produce the title of the page in the tab
   and a menu with the links to the pages of the documentation

.. raw html below is used to hide the title of the page but retain it in the 
   tab title. https://github.com/sphinx-doc/sphinx/issues/8356
.. raw:: html

   <div style="visibility: hidden;">

Home Page
===================================

.. raw:: html

   </div>

.. toctree::
   :hidden:

   Installation <install>
   User Guide <user_guide>
   Java API <java_api/library_root>
