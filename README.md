#agent-tools
A collection of tools to allow local & remote loading of agents into the JVM. 

Still in progress :

- Remote version is untested.
- Tests from client using it as agent-tools.jar.

--------
Overview
--------

It provides 10 methods :

(local version) -> Works on the local JVM.

load (ClassFileTransformer ctf) -> loads a ClassFileTransformer
remove (ClassFileTransformer ctf) -> removes a ClassFileTransformer
reset (String... classNames) -> loads the original version of the class (w/o any bytecode modifications)
retransform (ClassFileTransformer ctf, String... classNames) -> retransforms the classes using ctf
redefine(String... classNames) -> redefines the classes using the currently loaded ctfs.

(remote version) -> Same as local, but working on a remote JVM. 

load (ClassFileTransformer ctf, int pid) -> loads a ClassFileTransformer
remove (ClassFileTransformer ctf, int pid) -> removes a ClassFileTransformer
reset (int pid, String... classNames) -> loads the original version of the class (w/o any bytecode modifications)
retransform (int pid, ClassFileTransformer ctf, String... classNames) -> retransforms the classes using ctf
redefine(int pid, String... classNames) -> redefines the classes using the currently loaded ctfs.

---------
Specifics 
---------

Local :

AgentTools loads itself in the running JVM as .jar. 
Because everything needed is in CP already, the generated .jar contains the needed MANIFEST.MF only.

Remote :

AgentTools creates a .jar with the required classes and loads into the alien VM. 
Inside the alien VM, the agent.jar creates a MBean which facades the functionality.
Back in the initial VM, any request to the remote methods, opens a JMX connection with the alien VM and communicates with the MBean.

