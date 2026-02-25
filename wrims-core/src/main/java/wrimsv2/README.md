# Purpose of This Module

**To retain support for legacy code.** This module contains code related to calling the external 
native functions present in the CalLite ANN interface for the model loader. 

Some of these functions are found in the source of CalLite here: 
https://github.com/CentralValleyModeling/CalLite/tree/master/Run/External/wrimsv2/external.
Other functions are crafted for the specific needs of the model and are included in the model
project files themselves.

To see the usage of these functions, see the legacy test code found in WRIMS 2 available here:
https://github.com/CentralValleyModeling/wrims/blob/Feature/wrims-devops/wrims_v2/wrims_v2/src/test/test_external/TestExternalFunction.java

The ANN interface is integral to CalLite and the code of the models themselves. To 
maintain compatibility with existing models, the presence of this module in the original
package structure is necessary (`wrimsv2`). The external function native calls rely on the calling classes
and the target native classes sharing the same package structure, which is only possible by
retaining this module.

The ANN interface can be generated using a definition file such as the one found here:
https://github.com/CentralValleyModeling/wrims/blob/Feature/wrims-devops/wrims_v2/wrims_v2/src/test/test_external/generateInerfaceAnn.txt

Eventually, the native functions, the ANN interface, and this module should all be updated to use
the newer package structure (`gov.ca.water.wrims.engine`). This will require updating CalLite 
as well as the models themselves. A conversion utility may be considered to provide a smooth
transition, as existing models will no longer be compatible with WRIMS once the package structure
is changed.