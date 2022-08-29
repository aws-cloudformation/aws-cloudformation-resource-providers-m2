# AWS::M2::Environment EfsStorageConfiguration

Defines the storage configuration for an Amazon EFS file system.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#filesystemid" title="FileSystemId">FileSystemId</a>" : <i>String</i>,
    "<a href="#mountpoint" title="MountPoint">MountPoint</a>" : <i>String</i>
}
</pre>

### YAML

<pre>
<a href="#filesystemid" title="FileSystemId">FileSystemId</a>: <i>String</i>
<a href="#mountpoint" title="MountPoint">MountPoint</a>: <i>String</i>
</pre>

## Properties

#### FileSystemId

The file system identifier.

_Required_: Yes

_Type_: String

_Pattern_: <code>^\S{1,200}$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### MountPoint

The mount point for the file system.

_Required_: Yes

_Type_: String

_Pattern_: <code>^\S{1,200}$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

