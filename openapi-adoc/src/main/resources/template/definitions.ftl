<#if components?? && (components.getSchemas()?has_content || components.getSecuritySchemes()?has_content)>
[[_components]]
== Components
  <#if components.getSchemas()?has_content>

[[_components_schemas]]
=== Schemas
    <#list components.getSchemas() as schemaName, schema>

[[_components_schemas_${schemaName}]]
==== ${schemaName}
      <#if schema.getProperties()?has_content>
        <#assign schemaProperties = schema.getProperties() />
        <#include template_properties />
      </#if>
    </#list>
  </#if>

  <#if components.getSecuritySchemes()?has_content>

[[_components_securitySchemes]]
=== Security Schemes
[%header,caption=,cols=".^2a,.^4a,.^4a,.^16a"]
|===
<.<|Name
<.<|Type
<.<|In
<.<|Description
    <#list components.getSecuritySchemes() as securitySchemaName, securitySchema>

<.<|*${securitySchemaName}*
<.<|${securitySchema.getType()?has_content?then(securitySchema.getType(), '')}
<.<|${securitySchema.getIn()?has_content?then(securitySchema.getIn(), '')}
<.<|${securitySchema.getDescription()?has_content?then(securitySchema.getDescription(), '')} +
      <#if securitySchema.getOpenIdConnectUrl()?has_content>
${securitySchema.getOpenIdConnectUrl()}[OpenID connect URL]
      </#if>
      <#if securitySchema.getBearerFormat()?has_content>
*Bearer format:* ${securitySchema.getBearerFormat()}
      </#if>
      <#if securitySchema.getFlows()??>
        <#if securitySchema.getFlows().getImplicit()??>
          <#assign oauthType = "implicit" />
          <#assign flow = securitySchema.getFlows().getImplicit() />
        <#elseif securitySchema.getFlows().getPassword()??>
          <#assign oauthType = "password" />
          <#assign flow = securitySchema.getFlows().getPassword() />
        <#elseif securitySchema.getFlows().getClientCredentials()??>
          <#assign oauthType = "client-credential" />
          <#assign flow = securitySchema.getFlows().getClientCredentials() />
        <#elseif securitySchema.getFlows().getAuthorizationCode()??>
          <#assign oauthType = "authorization_code" />
          <#assign flow = securitySchema.getFlows().getAuthorizationCode() />
        </#if>
OAuth flow: __${oauthType}__ +
        <#if flow??>
          <#if flow.getAuthorizationUrl()?has_content>
${flow.getAuthorizationUrl()}[Authorization URL] +
          </#if>
          <#if flow.getTokenUrl()?has_content>
${flow.getTokenUrl()}[Token URL] +
          </#if>
          <#if flow.getRefreshUrl()?has_content>
${flow.getRefreshUrl()}[Refresh URL] +
          </#if>
          <#if flow.getScopes()?has_content>

*Scopes:*

            <#list flow.getScopes() as scope, description>
. `${scope}` : ${description}
            </#list>
          </#if>
        </#if>
      </#if>
    </#list>
|===
  </#if>
</#if>
