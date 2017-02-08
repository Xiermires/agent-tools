#agent-tools
A collection of tools to allow local & remote loading of agents into the JVM. 

--------
Overview
--------

It provides 11 methods :

(local version) -> Works on the local JVM.

* load (ClassFileTransformer ctf) -> loads a ClassFileTransformer
* remove (ClassFileTransformer ctf) -> removes a ClassFileTransformer
* reset (String... classNames) -> loads the original version of the class (w/o any bytecode modifications)
* retransform (ClassFileTransformer ctf, String... classNames) -> retransforms the classes using ctf
* redefine(String... classNames) -> redefines the classes using the currently loaded ctfs

(remote version) -> Same as local, but working on a remote JVM. 

* load (int pid, ClassFileTransformer ctf) -> see above
* remove (int pid, ClassFileTransformer ctf) -> see above
* reset (int pid, String... classNames) -> see above
* retransform (int pid, ClassFileTransformer see above
* redefine(int pid, String... classNames) -> see above
* loadJar(int pid, String jarName, byte[] jarBytes) -> loads the .jar in the remote VM class path 

---------
Specifics 
---------

Local :

AgentTools loads itself in the running JVM as .jar. 
Because everything needed is in CP already, the generated .jar contains the needed MANIFEST.MF only.

Remote :

AgentTools creates a .jar with the required classes and loads into the alien VM. 
Inside the alien VM, the agent.jar creates a MBean which facades the functionality.
Back in the initial VM, we JMX proxy the MBean to access the functionality.

