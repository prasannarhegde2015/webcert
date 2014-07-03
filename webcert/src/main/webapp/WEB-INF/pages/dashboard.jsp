<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<!DOCTYPE html>
<html lang="sv" id="ng-app" ng-app="webcert">
<head>

<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=Edge" />
<meta name="ROBOTS" content="nofollow, noindex" />
<meta name="viewport" content="width=device-width, initial-scale=1">

<title><spring:message code="application.name" /></title>

<link rel="stylesheet" href="/web/webjars/bootstrap/3.1.1/css/bootstrap.min.css">
<link rel="stylesheet" href="/web/webjars/bootstrap/3.1.1/css/bootstrap-theme.min.css">
<link rel="stylesheet" href="/web/webjars/common/webcert/css/inera-webcert.css?<spring:message code="buildNumber" />"
      media="screen">
<link rel="stylesheet" href="/web/webjars/common/css/inera-certificate.css?<spring:message code="buildNumber" />"
      media="screen">
<link rel="stylesheet" href="/web/webjars/common/webcert/css/print.css?<spring:message code="buildNumber" />"
      media="print">

<script type="text/javascript">
  // Global JS config/constants for this app, to be used by scripts
  var MODULE_CONFIG = {
    BUILD_NUMBER: '<spring:message code="buildNumber" />',
    USERCONTEXT: <sec:authentication property="principal.asJson" htmlEscape="false"/>,
    REQUIRE_DEV_MODE: '<c:out value="${requireDevMode}"/>'
  }
</script>

</head>
<body>
  <%-- ng-view that holds dynamic content managed by angular app --%>
  <div id="view" ng-view autoscroll="true"></div>

  <%-- No script to show at least something when javascript is off --%>
  <noscript>
    <h1>
      <span><spring:message code="error.noscript.title" /></span>
    </h1>
    <div class="alert alert-danger">
      <spring:message code="error.noscript.text" />
    </div>
  </noscript>

  <c:choose>
    <c:when test="${requireDevMode == 'true'}">
      <script type="text/javascript" data-main="/js/main" src="/web/webjars/requirejs/2.1.10/require.js"></script>
    </c:when>
    <c:otherwise>
      <script type="text/javascript" data-main="/js/main.min.js?<spring:message code="buildNumber" />"
              src="/web/webjars/requirejs/2.1.10/require.js"></script>
    </c:otherwise>
  </c:choose>
  <script type="text/javascript" src="/vendor/netid-1.0.js"></script>
  <script type="text/javascript" src="/siths.jsp"></script>
  <!--[if lte IE 8]>
  <script type="text/javascript" src="/web/webjars/respond/1.4.2/src/respond.js"></script>
  <![endif]-->
</body>
</html>
