<#if content??>
.Content
  <#list content as mediaTypeName, mediaType>
      <#if mediaType.getExample()?has_content><#assign example = mediaType.getExample() /><#include template_examples /></#if>
      <#assign schemaType = mediaType.getSchema() />
${mediaTypeName}:: <#include template_schemaType />

      <#assign propSchema = schemaType />
      <#include template_propertyDescription />
      <#if mediaType.getEncoding()?has_content>
        <#list mediaType.getEncoding() as encodingName, encoding>

==== ${encodingName}
          <#if encoding.getContentType()?has_content>
*Content-Type:* ${encoding.getContentType()} +
          </#if>
          <#if encoding.getExplode()?? && encoding.getExplode()>
*Style:* ${encoding.getStyle()} +
          </#if>
          <#if encoding.getExplode()?? && encoding.getExplode()>
__explode__ +
          </#if>
          <#if encoding.getAllowReserved()?? && encoding.getAllowReserved()>
__allow reserved__ +
          </#if>
          <#if encoding.getHeaders()?has_content>

            <#assign headers = encoding.getHeaders() />
            <#include template_headers />
          </#if>
        </#list>
      </#if>
  </#list>
</#if>
