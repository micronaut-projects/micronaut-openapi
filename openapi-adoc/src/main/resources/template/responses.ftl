<#if responses?has_content>

.Responses
[%header,caption=,cols=".^2a,.^14a,.^4a"]
|===
<.<|Code
<.<|Description
<.<|Links

  <#list responses as code, response>
<.<|${code}
<.<|<#compress>
    <#if response.get$ref()?has_content>
      <#assign response = response.get$ref()?remove_beginning("#/components/responses/") />
    </#if>
    <#if response.getDescription()?has_content>${response.getDescription()}</#if>
    </#compress>
    <#if response.getHeaders()?has_content>

      <#assign headers = response.getHeaders() />
      <#include template_headers />
    </#if>
    <#if response.getContent()?has_content>
      <#assign content = response.getContent() />
      <#include template_content />
    </#if>

<.<|<#assign links = response.getLinks()?has_content?then(response.getLinks(), []) /><#include template_links />
  </#list>
|===
</#if>
