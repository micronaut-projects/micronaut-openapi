<#if schemaProperties?has_content>
.Properties
[%header,caption=,cols=".^4a,.^16a,.^4a"]
|===
<.<|Name
<.<|Description
<.<|Type
<#list schemaProperties as propName, propSchema>

<.<|${propName}
<#if propSchema.getRequired()?has_content && propSchema.getRequired()?seq_contains(propName)>
__required__
</#if>
<.<|<#include template_propertyDescription />
<.<|<#assign schemaType = propSchema /><#include template_schemaType />
</#list>
|===
</#if>
