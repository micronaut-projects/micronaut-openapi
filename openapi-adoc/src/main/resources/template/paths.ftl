<#if paths?has_content>

== Paths
  <#list paths as pathsStr, path>
    <#list path.readOperationsMap() as method, operation>

=== __${method}__ `${pathsStr}`<#if operation.getSummary()?has_content> ${operation.getSummary()?trim}</#if>
      <#if operation.getDescription()?has_content>
${operation.getDescription()?trim}

      </#if>
      <#if operation.getRequestBody()??>
        <#assign requestBody = operation.getRequestBody() />
        <#include template_requestBody />
      </#if>
      <#if operation.getParameters()?has_content>
        <#assign parameters = operation.getParameters() />
        <#include template_parameters />
      </#if>
      <#if operation.getResponses()?has_content>
        <#assign responses = operation.getResponses() />
        <#include template_responses />
      </#if>
      <#assign securityReqs = operation.getSecurity()?has_content?then(operation.getSecurity(), []) />
      <#include template_securityRequirements />
    </#list>
  </#list>
</#if>
