# Millr

<p>Millr is a program that takes as input *.class, *.jar or folders, an output folder, and writes mokapot-friendly versions of the input program into the output folder.
Typical use consists in:
<ol>
<li>writing an application on java that relies on mokapot for distribution. </li>
<li>Compiling the application using any standard java compiler.</li>
<li>Running millr with, as input, the folder where the compiled classes are.</li>
<li> Running the transformed application</li>
</ol>
</p>


<p>The role of millr is to bypass the limitations of mokapot by creating a program with same semantic as the original one, but that relies on slightly different mechanisms. For instance, any direct access to fields are automatically replaced by calls to getter/setter methods. In order for millr to work as effectively as possible, it is advised to mill as much as possible dependencies of the original program alongside it, as a call to a method that has not been milled might result in a unexpected behaviour. However, milling the API is not possible, for security - implementation reasons. Therefore, milling will not remove every limitation from mokapot. Whenever millr encounters a limitation that he cannot remove, it will display a meaningful message to the user (only partially implemented at the moment). </p>

<p>
Right now, millr will try to remove the following limitations:
<ol>
<li>Allowing every classes to be extendable by removing final keywords, and changing visibility of methods appropriately.</li>
<li>Allowing arrays to be migrated by transforming them into "normal" objects with access through appropriate methods.</li>
<li>Allowing lambdas to be migrated by transforming them into anonymous classes.</li>
<li>Allowing methods that access fields from others objects to run smoothly by replacing every direct access with getter-setter methods.</li>
<li>Allowing the user the use the synchronized keyword on objects by implementing methods allowing once to gain access to the lock of the object.</li>
<li>Allowing comparisons of objects for reference equality, or objects' actual classes for equality, by calling mokapot’s LengthIndependent methods automatically. </li>
</ol>
</p>

<p>At the moment, millr will change the name of every classes / *.jar it mills with _millr_ at the start of the name, but will also copy the original classes into the output folder.  Therefore, caution should be used when calling the milled program in the output folder to use the new name, otherwise it will result in a call to the original one. </p>
  

<p>
Millr should be from the command line as follows:

<code> ant run-millr -Dinput=input-files -Doutput=output-directory </code> 

from the mokapot-combined root repository.
The output must consist of a single folder, the inputs might be either classes, *.jar files, or folders. The inputs must not contains two classes with the same name, and *.jar files must not contain other *.jar files inside it. 
</p>
