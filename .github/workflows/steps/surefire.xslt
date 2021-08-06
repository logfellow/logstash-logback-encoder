<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" encoding="UTF-8" omit-xml-declaration="no" cdata-section-elements="failure rerunFailure flakyFailure error rerunError flakyError system-out system-err"/> 

    <!-- Copy the entire document
     -->
    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>
    
    <!-- Copy a specific element
     -->
    <xsl:template match="failure/text()">
        <xsl:value-of select="../../system-out" />
        <xsl:value-of select="." />
    </xsl:template>

</xsl:stylesheet> 