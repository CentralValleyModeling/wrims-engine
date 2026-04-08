.. _home:

WRIMS Engine
============

.. note::

   This documentation is a work in progress. Pages are being added over time.
   If you need guidance on a topic not yet covered here, please reach out to
   your project team or consult the in-application help.

Getting Started
---------------

If you are new to WRIMS, the recommended path is:

1. Install WRIMS GUI. Starting out with WRIMS Engine is probably not the best
   entry point.
2. Open an existing study in the GUI to familiarise yourself with the project
   structure and configuration files.
3. When you are ready to run studies programmatically or in batch, refer to the
   headless execution guide below.

Contents
--------

.. toctree::
   :maxdepth: 1
   :caption: How-To Guides

   running_headless_wrims_engine

.. toctree::
   :maxdepth: 1
   :caption: Reference

   configuration

.. note::

   The **Configuration Reference** page is not yet written. The entry above is
   a placeholder to show where it will appear in the navigation.

About WRIMS
-----------

WRIMS is developed and maintained by the `California Department of Water
Resources <https://water.ca.gov/>`_. It is primarily used for planning and
operational studies of the State Water Project and the Central Valley water
system, though its general modeling framework can be applied to other
large-scale water resource networks.

The engine solves a linear program at each cycle/timestep to determine optimal
allocations and deliveries given the network topology, physical constraints,
and operational priorities defined in the study configuration.
