<#assign allSecurityReqs = globalSecurityRequirements?has_content?then(globalSecurityRequirements, []) />
<#assign allSecurityReqs += securityReqs />
<#if allSecurityReqs?has_content>

.Security
[%header,caption=,cols=".^3a,.^4a,.^13a"]
|===
<.<|Name
<.<|Type
<.<|Scopes
  <#list allSecurityReqs as securityReq>
    <#list securityReq as securityReqName, securityReqScopes>

<.<|${securityReqName}
<.<|**${securityReqScopes?has_content?then("oauth2", "apiKey")}**
<.<|${securityReqScopes?join(", ")}
    </#list>
  </#list>
|===
</#if>
