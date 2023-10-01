<#if links?has_content>
  <#list links as name, link>
    <#if link.get$ref()?has_content && components?? && components.getLinks()?has_content>
        <#assign l = components.getLinks()[link.get$ref()?remove_beginning("#/components/links/")] />
    <#else>
        <#assign l = link />
    </#if>
*${name}* +
      <#if l.getDescription()?has_content>

${l.getDescription()}
      </#if>
      <#if l.getOperationId()?has_content>

__Operation__: `${l.getOperationId()}` +
      </#if>
      <#if l.getParameters()?has_content>

__Parameters__: { +
          <#list l.getParameters() as name, value>
"${name}": "${value}"${name?has_next?then(',', '')} +
          </#list>
} +
      </#if>
  </#list>
<#else>
No links
</#if>
