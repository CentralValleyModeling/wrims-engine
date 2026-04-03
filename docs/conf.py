# Configuration file for the Sphinx documentation builder.

# -- Project information

project = "wrims-engine"
copyright = "2026, CA Department of Water Resources"
author = "Zachary Roy"

release = "3.0"
version = "3.0.0"

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
    "wrims-docs": (
        "https://wrims-docs.readthedocs.io/en/latest",
        None,
    ),
    "wrims-gui": (
        "https://wrims-docs.readthedocs.io/projects/wrims-gui/en/latest",
        None,
    ),
    "wresl": (
        "https://wrims-docs.readthedocs.io/projects/wresl/en/latest",
        None,
    ),
}
intersphinx_disabled_reftypes = ["std:doc", "std:label"]

templates_path = ["_templates"]

# -- Options for HTML output

html_theme = "sphinx_rtd_theme"
html_theme_options = {
    "navigation_depth": 2,
}
github_url = "https://github.com/CentralValleyModeling/wrims-engine"


# -- Options for EPUB output
epub_show_urls = "footnote"
