<#if servers?has_content>

== Servers
  <#list servers as server>

=== __Server__: ${server.getUrl()}
    <#if server.getDescription()?has_content>

${server.getDescription()}
    </#if>
    <#if server.getVariables()?has_content>

.Server Variables
[%header,caption=,cols=".^2a,.^9a,.^3a,.^4a"]
|===
<.<|Variable
<.<|Description
<.<|Possible Values
<.<|Default
      <#list server.getVariables() as varName, var>

<.<|${varName}
<.<|${var.getDescription()}
<.<|${var.getEnum()?has_content?then(var.getEnum()?join(", "), "Any")}
<.<|${var.getDefault()}
      </#list>
|===
    </#if>
  </#list>
</#if>
