# Configuration file for the Sphinx documentation builder.

# -- Project information

project = "wrims-engine"
copyright = "2026, CA Department of Water Resources"
author = "Zachary Roy"

release = "0.1"
version = "0.1.0"

# -- General configuration

extensions = [
    "sphinx.ext.duration",
    "sphinx.ext.doctest",
    "sphinx.ext.autodoc",
    "sphinx.ext.autosummary",
    "sphinx.ext.intersphinx",
    "myst_parser",
]

intersphinx_mapping = {
    "wrims-docs": ("https://www.sphinx-doc.org/en/master/", None),
    "wrims-gui": ("https://www.sphinx-doc.org/en/master/", None),
    "wresl": ("https://www.sphinx-doc.org/en/master/", None),
}
intersphinx_disabled_reftypes = ["*"]

templates_path = ["_templates"]

# -- Options for HTML output

html_theme = "sphinx_rtd_theme"

# -- Options for EPUB output
epub_show_urls = "footnote"


