<#if externalDocs?has_content>
${externalDocs.getUrl()?has_content?then(externalDocs.getUrl() + '[' + externalDocs.getDescription() + ']', externalDocs.getDescription())} +
</#if>
