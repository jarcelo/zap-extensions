/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2016 The ZAP Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.zap.extension.ascanrules;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.zaproxy.zap.extension.ascanrules.utils.Constants.NULL_BYTE_CHARACTER;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.core.scanner.Alert;
import org.parosproxy.paros.core.scanner.Plugin.AlertThreshold;
import org.parosproxy.paros.core.scanner.ScannerParam;
import org.parosproxy.paros.network.HtmlParameter;
import org.parosproxy.paros.network.HttpMalformedHeaderException;
import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.network.HttpRequestHeader;
import org.zaproxy.addon.commonlib.CommonAlertTag;
import org.zaproxy.zap.testutils.NanoServerHandler;
import org.zaproxy.zap.utils.ZapXmlConfiguration;

class CrossSiteScriptingScanRuleUnitTest extends ActiveScannerTest<CrossSiteScriptingScanRule> {

    @Override
    protected CrossSiteScriptingScanRule createScanner() {
        return new CrossSiteScriptingScanRule();
    }

    @Test
    void shouldReturnExpectedMappings() {
        // Given / When
        int cwe = rule.getCweId();
        int wasc = rule.getWascId();
        Map<String, String> tags = rule.getAlertTags();
        // Then
        assertThat(cwe, is(equalTo(79)));
        assertThat(wasc, is(equalTo(8)));
        assertThat(tags.size(), is(equalTo(2)));
        assertThat(
                tags.containsKey(CommonAlertTag.OWASP_2021_A03_INJECTION.getTag()),
                is(equalTo(true)));
        assertThat(
                tags.containsKey(CommonAlertTag.OWASP_2017_A01_INJECTION.getTag()),
                is(equalTo(true)));
        assertThat(
                tags.get(CommonAlertTag.OWASP_2021_A03_INJECTION.getTag()),
                is(equalTo(CommonAlertTag.OWASP_2021_A03_INJECTION.getValue())));
        assertThat(
                tags.get(CommonAlertTag.OWASP_2017_A01_INJECTION.getTag()),
                is(equalTo(CommonAlertTag.OWASP_2017_A01_INJECTION.getValue())));
    }

    @Test
    void shouldReportXssInParagraph() throws NullPointerException, IOException {
        String test = "/shouldReportXssInParagraph/";

        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String name = getFirstParamValue(session, "name");
                        String response;
                        if (name != null) {
                            response =
                                    getHtml(
                                            "InputInParagraph.html",
                                            new String[][] {{"name", name}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });

        HttpMessage msg = this.getHttpMessage(test + "?name=test");

        this.rule.init(msg, this.parent);

        this.rule.scan();

        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(alertsRaised.get(0).getEvidence(), equalTo("</p><script>alert(1);</script><p>"));
        assertThat(alertsRaised.get(0).getParam(), equalTo("name"));
        assertThat(alertsRaised.get(0).getAttack(), equalTo("</p><script>alert(1);</script><p>"));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_HIGH));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_MEDIUM));
    }

    @Test
    void shouldReportXssInParagraphForNullBytePayloadInjection()
            throws NullPointerException, IOException {
        String test = "/shouldReportXssInParagraphForNullByteInjection/";
        // Given
        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String name = getFirstParamValue(session, "name");
                        String response;
                        if (name != null
                                && (name.contains(NULL_BYTE_CHARACTER)
                                        || name.equals(Constant.getEyeCatcher()))) {
                            response =
                                    getHtml(
                                            "InputInParagraph.html",
                                            new String[][] {{"name", name}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });

        // When
        HttpMessage msg = this.getHttpMessage(test + "?name=test");

        this.rule.init(msg, this.parent);
        this.rule.scan();

        // Then
        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(
                alertsRaised.get(0).getEvidence(),
                equalTo("</p>" + NULL_BYTE_CHARACTER + "<script>alert(1);</script><p>"));
        assertThat(alertsRaised.get(0).getParam(), equalTo("name"));
        assertThat(
                alertsRaised.get(0).getAttack(),
                equalTo("</p>" + NULL_BYTE_CHARACTER + "<script>alert(1);</script><p>"));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_HIGH));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_MEDIUM));
    }

    @Test
    void shouldNotReportXssInFilteredParagraph() throws NullPointerException, IOException {
        String test = "/shouldNotReportXssInFilteredParagraph/";

        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String name = getFirstParamValue(session, "name");
                        String response;
                        if (name != null) {
                            // Strip out suitable nasties
                            name =
                                    name.replaceAll("<", "")
                                            .replaceAll(">", "")
                                            .replaceAll("&", "")
                                            .replaceAll("#", "");
                            response =
                                    getHtml(
                                            "InputInParagraph.html",
                                            new String[][] {{"name", name}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });

        HttpMessage msg = this.getHttpMessage(test + "?name=test");

        this.rule.init(msg, this.parent);

        this.rule.scan();

        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    void shouldReportXssInComment() throws NullPointerException, IOException {
        String test = "/shouldReportXssInComment/";

        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String name = getFirstParamValue(session, "name");
                        String response;
                        if (name != null) {
                            response =
                                    getHtml("InputInComment.html", new String[][] {{"name", name}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });

        HttpMessage msg = this.getHttpMessage(test + "?name=test");

        this.rule.init(msg, this.parent);

        this.rule.scan();

        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(alertsRaised.get(0).getEvidence(), equalTo("--><script>alert(1);</script><!--"));
        assertThat(alertsRaised.get(0).getParam(), equalTo("name"));
        assertThat(alertsRaised.get(0).getAttack(), equalTo("--><script>alert(1);</script><!--"));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_HIGH));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_MEDIUM));
    }

    @Test
    void shouldReportXssInCommentForNullBytePayloadInjection()
            throws NullPointerException, IOException {
        String test = "/shouldReportXssInCommentForNullBytePayloadInjection/";
        // Given
        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String name = getFirstParamValue(session, "name");
                        String response;
                        if (name != null
                                && (name.contains(NULL_BYTE_CHARACTER)
                                        || name.equals(Constant.getEyeCatcher()))) {
                            response =
                                    getHtml("InputInComment.html", new String[][] {{"name", name}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });

        // When
        HttpMessage msg = this.getHttpMessage(test + "?name=test");

        this.rule.init(msg, this.parent);
        this.rule.scan();

        // Then
        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(
                alertsRaised.get(0).getEvidence(),
                equalTo("-->" + NULL_BYTE_CHARACTER + "<script>alert(1);</script><!--"));
        assertThat(alertsRaised.get(0).getParam(), equalTo("name"));
        assertThat(
                alertsRaised.get(0).getAttack(),
                equalTo("-->" + NULL_BYTE_CHARACTER + "<script>alert(1);</script><!--"));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_HIGH));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_MEDIUM));
    }

    @Test
    void shouldReportXssInCommentWithFilteredScripts() throws NullPointerException, IOException {
        String test = "/shouldReportXssInCommentWithFilteredScripts/";

        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String name = getFirstParamValue(session, "name");
                        String response;
                        if (name != null) {
                            // Strip out 'script' ignoring the case
                            name = name.replaceAll("(?i)script", "");
                            response =
                                    getHtml("InputInComment.html", new String[][] {{"name", name}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });

        HttpMessage msg = this.getHttpMessage(test + "?name=test");

        this.rule.init(msg, this.parent);

        this.rule.scan();

        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(
                alertsRaised.get(0).getEvidence(),
                equalTo("--><b onMouseOver=alert(1);>test</b><!--"));
        assertThat(alertsRaised.get(0).getParam(), equalTo("name"));
        assertThat(
                alertsRaised.get(0).getAttack(),
                equalTo("--><b onMouseOver=alert(1);>test</b><!--"));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_HIGH));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_MEDIUM));
    }

    @Test
    void shouldNotReportXssInFilteredComment() throws NullPointerException, IOException {
        String test = "/shouldNotReportXssInFilteredComment/";

        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String name = getFirstParamValue(session, "name");
                        String response;
                        if (name != null) {
                            // Strip out suitable nasties
                            name =
                                    name.replaceAll("<", "")
                                            .replaceAll(">", "")
                                            .replaceAll("&", "")
                                            .replaceAll("#", "");
                            response =
                                    getHtml("InputInComment.html", new String[][] {{"name", name}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });

        HttpMessage msg = this.getHttpMessage(test + "?name=test");

        this.rule.init(msg, this.parent);

        this.rule.scan();

        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    void shouldReportXssInBody() throws NullPointerException, IOException {
        String test = "/shouldReportXssInBody/";

        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String name = getFirstParamValue(session, "name");
                        String response;
                        if (name != null) {
                            response = getHtml("InputInBody.html", new String[][] {{"name", name}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });

        HttpMessage msg = this.getHttpMessage(test + "?name=test");

        this.rule.init(msg, this.parent);

        this.rule.scan();

        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(alertsRaised.get(0).getEvidence(), equalTo("<script>alert(1);</script>"));
        assertThat(alertsRaised.get(0).getParam(), equalTo("name"));
        assertThat(alertsRaised.get(0).getAttack(), equalTo("<script>alert(1);</script>"));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_HIGH));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_MEDIUM));
    }

    @Test
    void shouldReportXssInBodyForNullByteBasedInjectionPayload()
            throws NullPointerException, IOException {
        String test = "/shouldReportXssInBodyForNullByteBasedInjectionPayload/";
        // Given
        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String name = getFirstParamValue(session, "name");
                        String response;
                        if (name != null
                                && (name.contains(NULL_BYTE_CHARACTER)
                                        || name.equals(Constant.getEyeCatcher()))) {
                            response = getHtml("InputInBody.html", new String[][] {{"name", name}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });
        // When
        HttpMessage msg = this.getHttpMessage(test + "?name=test");

        this.rule.init(msg, this.parent);

        this.rule.scan();
        // Then
        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(
                alertsRaised.get(0).getEvidence(),
                equalTo(NULL_BYTE_CHARACTER + "<script>alert(1);</script>"));
        assertThat(alertsRaised.get(0).getParam(), equalTo("name"));
        assertThat(
                alertsRaised.get(0).getAttack(),
                equalTo(NULL_BYTE_CHARACTER + "<script>alert(1);</script>"));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_HIGH));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_MEDIUM));
    }

    @Test
    void shouldReportXssInSpanContent() throws NullPointerException, IOException {
        String test = "/shouldReportXssInSpanContent/";

        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String name = getFirstParamValue(session, "name");
                        String response;
                        if (name != null) {
                            response = getHtml("InputInSpan.html", new String[][] {{"name", name}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });

        HttpMessage msg = this.getHttpMessage(test + "?name=test");

        this.rule.init(msg, this.parent);

        this.rule.scan();

        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(
                alertsRaised.get(0).getEvidence(),
                equalTo("</span><script>alert(1);</script><span>"));
        assertThat(alertsRaised.get(0).getParam(), equalTo("name"));
        assertThat(
                alertsRaised.get(0).getAttack(),
                equalTo("</span><script>alert(1);</script><span>"));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_HIGH));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_MEDIUM));
    }

    @Test
    void shouldReportXssInSpanContentForNullByteInjectionPayload()
            throws NullPointerException, IOException {
        String test = "/shouldReportXssInSpanContentForNullByteInjectionPayload/";
        // Given
        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String name = getFirstParamValue(session, "name");
                        String response;
                        if (name != null
                                && (name.contains(NULL_BYTE_CHARACTER)
                                        || name.equals(Constant.getEyeCatcher()))) {
                            response = getHtml("InputInSpan.html", new String[][] {{"name", name}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });
        // When
        HttpMessage msg = this.getHttpMessage(test + "?name=test");

        this.rule.init(msg, this.parent);
        this.rule.scan();
        // Then
        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(
                alertsRaised.get(0).getEvidence(),
                equalTo("</span>" + NULL_BYTE_CHARACTER + "<script>alert(1);</script><span>"));
        assertThat(alertsRaised.get(0).getParam(), equalTo("name"));
        assertThat(
                alertsRaised.get(0).getAttack(),
                equalTo("</span>" + NULL_BYTE_CHARACTER + "<script>alert(1);</script><span>"));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_HIGH));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_MEDIUM));
    }

    @Test
    void shouldReportXssOutsideOfTags() throws NullPointerException, IOException {
        String test = "/shouldReportXssOutsideOfTags/";

        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String name = getFirstParamValue(session, "name");
                        String response;
                        if (name != null) {
                            response = getHtml("InputIsBody.html", new String[][] {{"name", name}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });

        HttpMessage msg = this.getHttpMessage(test + "?name=test");

        this.rule.init(msg, this.parent);

        this.rule.scan();

        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(alertsRaised.get(0).getEvidence(), equalTo("<script>alert(1);</script>"));
        assertThat(alertsRaised.get(0).getParam(), equalTo("name"));
        assertThat(alertsRaised.get(0).getAttack(), equalTo("<script>alert(1);</script>"));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_HIGH));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_MEDIUM));
    }

    @Test
    void shouldReportXssOutsideOfHtmlTags() throws NullPointerException, IOException {
        String test = "/shouldReportXssOutsideOfHtmlTags/";

        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String name = getFirstParamValue(session, "name");
                        String response;
                        if (name != null) {
                            response =
                                    getHtml(
                                            "InputOutsideHtmlTag.html",
                                            new String[][] {{"name", name}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });

        HttpMessage msg = this.getHttpMessage(test + "?name=test");

        this.rule.init(msg, this.parent);

        this.rule.scan();

        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(alertsRaised.get(0).getEvidence(), equalTo("<script>alert(1);</script>"));
        assertThat(alertsRaised.get(0).getParam(), equalTo("name"));
        assertThat(alertsRaised.get(0).getAttack(), equalTo("<script>alert(1);</script>"));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_HIGH));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_MEDIUM));
    }

    @Test
    void shouldReportXssOutsideOfHtmlTagsForNullByteBasedInjection()
            throws NullPointerException, IOException {
        String test = "/shouldReportXssOutsideOfHtmlTagsForNullByteBasedInjection/";
        // Given
        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String name = getFirstParamValue(session, "name");
                        String response;
                        if (name != null
                                && (name.contains(NULL_BYTE_CHARACTER)
                                        || name.equals(Constant.getEyeCatcher()))) {
                            response =
                                    getHtml(
                                            "InputOutsideHtmlTag.html",
                                            new String[][] {{"name", name}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });

        // When
        HttpMessage msg = this.getHttpMessage(test + "?name=test");

        this.rule.init(msg, this.parent);

        this.rule.scan();
        // Then
        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(alertsRaised.get(0).getEvidence(), equalTo("<script>alert(1);</script>"));
        assertThat(alertsRaised.get(0).getParam(), equalTo("name"));
        assertThat(alertsRaised.get(0).getAttack(), equalTo("<script>alert(1);</script>"));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_HIGH));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_MEDIUM));
    }

    @Test
    void shouldReportXssInBodyWithFilteredScript() throws NullPointerException, IOException {
        String test = "/shouldReportXssInBodyWithFilteredScript/";

        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String name = getFirstParamValue(session, "name");
                        String response;
                        if (name != null) {
                            // Strip out 'script' ignoring the case
                            name = name.replaceAll("(?i)script", "");
                            response = getHtml("InputInBody.html", new String[][] {{"name", name}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });

        HttpMessage msg = this.getHttpMessage(test + "?name=test");

        this.rule.init(msg, this.parent);

        this.rule.scan();

        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(alertsRaised.get(0).getEvidence(), equalTo("<b onMouseOver=alert(1);>test</b>"));
        assertThat(alertsRaised.get(0).getParam(), equalTo("name"));
        assertThat(alertsRaised.get(0).getAttack(), equalTo("<b onMouseOver=alert(1);>test</b>"));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_HIGH));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_MEDIUM));
    }

    @Test
    void shouldNotReportXssInFilteredBody() throws NullPointerException, IOException {
        String test = "/shouldNotReportXssInFilteredBody/";

        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String name = getFirstParamValue(session, "name");
                        String response;
                        if (name != null) {
                            // Strip out suitable nasties
                            name =
                                    name.replaceAll("<", "")
                                            .replaceAll(">", "")
                                            .replaceAll("&", "")
                                            .replaceAll("#", "");
                            response = getHtml("InputInBody.html", new String[][] {{"name", name}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });

        HttpMessage msg = this.getHttpMessage(test + "?name=test");

        this.rule.init(msg, this.parent);

        this.rule.scan();

        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    void shouldReportXssInAttribute() throws NullPointerException, IOException {
        String test = "/shouldReportXssInAttribute/";

        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String color = getFirstParamValue(session, "color");
                        String response;
                        if (color != null) {
                            // Strip out < and >
                            color = color.replaceAll("<", "").replaceAll(">", "");
                            response =
                                    getHtml(
                                            "InputInAttribute.html",
                                            new String[][] {{"color", color}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });

        HttpMessage msg = this.getHttpMessage(test + "?color=red");

        this.rule.init(msg, this.parent);

        this.rule.scan();

        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(alertsRaised.get(0).getEvidence(), equalTo("\" onMouseOver=\"alert(1);"));
        assertThat(alertsRaised.get(0).getParam(), equalTo("color"));
        assertThat(alertsRaised.get(0).getAttack(), equalTo("\" onMouseOver=\"alert(1);"));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_HIGH));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_MEDIUM));
    }

    @Test
    void shouldNotReportXssInFilteredAttribute() throws NullPointerException, IOException {
        String test = "/shouldNotReportXssInFilteredAttribute/";

        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String color = getFirstParamValue(session, "color");
                        String response;
                        if (color != null) {
                            // Strip out suitable nasties
                            color =
                                    color.replaceAll("<", "")
                                            .replaceAll(">", "")
                                            .replaceAll("&", "")
                                            .replaceAll("#", "")
                                            .replaceAll("\"", "");
                            response =
                                    getHtml(
                                            "InputInAttribute.html",
                                            new String[][] {{"color", color}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });

        HttpMessage msg = this.getHttpMessage(test + "?color=red");

        this.rule.init(msg, this.parent);

        this.rule.scan();

        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    void shouldReportXssInAttributeScriptTag() throws NullPointerException, IOException {
        String test = "/shouldReportXssInAttributeScriptTag/";

        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String color = getFirstParamValue(session, "color");
                        String response;
                        if (color != null) {
                            // Strip out < and >
                            color = color.replaceAll("<", "").replaceAll(">", "");
                            response =
                                    getHtml(
                                            "InputInAttributeScriptTag.html",
                                            new String[][] {{"color", color}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });

        HttpMessage msg = this.getHttpMessage(test + "?color=red");

        this.rule.init(msg, this.parent);

        this.rule.scan();

        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(alertsRaised.get(0).getEvidence(), equalTo(";alert(1)"));
        assertThat(alertsRaised.get(0).getParam(), equalTo("color"));
        assertThat(alertsRaised.get(0).getAttack(), equalTo(";alert(1)"));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_HIGH));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_MEDIUM));
    }

    @Test
    void shouldReportXssInFrameSrcTag() throws NullPointerException, IOException {
        String test = "/shouldReportXssInFrameSrcTag/";

        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String name = getFirstParamValue(session, "name");
                        String response;
                        if (name != null) {
                            // Strip out < and >
                            name = name.replaceAll("<", "").replaceAll(">", "");
                            response =
                                    getHtml(
                                            "InputInFrameSrcTag.html",
                                            new String[][] {{"name", name}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });

        HttpMessage msg = this.getHttpMessage(test + "?name=file.html");

        this.rule.init(msg, this.parent);

        this.rule.scan();

        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(alertsRaised.get(0).getEvidence(), equalTo("javascript:alert(1);"));
        assertThat(alertsRaised.get(0).getParam(), equalTo("name"));
        assertThat(alertsRaised.get(0).getAttack(), equalTo("javascript:alert(1);"));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_HIGH));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_MEDIUM));
    }

    @Test
    void shouldReportXssInScriptIdTag() throws NullPointerException, IOException {
        String test = "/shouldReportXssInScriptIdTag/";

        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String name = getFirstParamValue(session, "name");
                        String response;
                        if (name != null) {
                            // Strip out < and >
                            name = name.replaceAll("<", "").replaceAll(">", "");
                            response =
                                    getHtml(
                                            "InputInScriptIdTag.html",
                                            new String[][] {{"name", name}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });

        HttpMessage msg = this.getHttpMessage(test + "?name=file.html");

        this.rule.init(msg, this.parent);

        this.rule.scan();

        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(alertsRaised.get(0).getEvidence(), equalTo(" src=http://badsite.com"));
        assertThat(alertsRaised.get(0).getParam(), equalTo("name"));
        assertThat(alertsRaised.get(0).getAttack(), equalTo(" src=http://badsite.com"));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_HIGH));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_MEDIUM));
    }

    @Test
    void shouldReportXssInReflectedUrl() throws NullPointerException, IOException {
        String test = "/shouldReportXssInReflectedUrl";

        NanoServerHandler handler =
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String url = session.getUri();
                        if (session.getQueryParameterString() != null) {
                            try {
                                url +=
                                        "?"
                                                + URLDecoder.decode(
                                                        session.getQueryParameterString(), "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                // At least this might be noticed
                                e.printStackTrace();
                            }
                        }

                        String response =
                                getHtml("ReflectedUrl.html", new String[][] {{"url", url}});
                        return newFixedLengthResponse(response);
                    }
                };

        this.nano.addHandler(handler);
        this.nano.setHandler404(handler);
        this.scannerParam.setAddQueryParam(true);

        HttpMessage msg = this.getHttpMessage(test);

        this.rule.init(msg, this.parent);

        this.rule.scan();

        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(alertsRaised.get(0).getEvidence(), equalTo("</p><script>alert(1);</script><p>"));
        assertThat(alertsRaised.get(0).getParam(), equalTo("query"));
        assertThat(alertsRaised.get(0).getAttack(), equalTo("</p><script>alert(1);</script><p>"));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_HIGH));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_MEDIUM));
    }

    @Test
    void shouldNotTestWhenMethodIsPutAndThresholdMedium() throws HttpMalformedHeaderException {
        // Given
        String test = "/shouldReportXssInReflectedUrl";

        NanoServerHandler handler =
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String url = session.getUri();
                        if (session.getQueryParameterString() != null) {
                            try {
                                url +=
                                        "?"
                                                + URLDecoder.decode(
                                                        session.getQueryParameterString(), "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                // At least this might be noticed
                                e.printStackTrace();
                            }
                        }

                        String response =
                                getHtml("ReflectedUrl.html", new String[][] {{"url", url}});
                        return newFixedLengthResponse(response);
                    }
                };

        this.nano.addHandler(handler);

        HttpMessage msg = this.getHttpMessage(test + "?name=test");
        msg.getRequestHeader().setMethod(HttpRequestHeader.PUT);

        rule.setConfig(new ZapXmlConfiguration());
        this.rule.setAlertThreshold(AlertThreshold.MEDIUM);
        this.rule.init(msg, this.parent);
        // When
        this.rule.scan();
        // Then
        assertThat(httpMessagesSent, hasSize(equalTo(0)));
    }

    @Test
    void shouldTestWhenMethodIsPutAndThresholdLow() throws HttpMalformedHeaderException {
        // Given
        String test = "/shouldReportXssInReflectedUrl";

        NanoServerHandler handler =
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String url = session.getUri();
                        if (session.getQueryParameterString() != null) {
                            try {
                                url +=
                                        "?"
                                                + URLDecoder.decode(
                                                        session.getQueryParameterString(), "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                // At least this might be noticed
                                e.printStackTrace();
                            }
                        }

                        String response =
                                getHtml("ReflectedUrl.html", new String[][] {{"url", url}});
                        return newFixedLengthResponse(response);
                    }
                };

        this.nano.addHandler(handler);

        HttpMessage msg = this.getHttpMessage(test + "?name=test");
        msg.getRequestHeader().setMethod(HttpRequestHeader.PUT);

        this.rule.setConfig(new ZapXmlConfiguration());
        this.rule.setAlertThreshold(AlertThreshold.LOW);
        this.rule.init(msg, this.parent);
        // When
        this.rule.scan();
        // Then
        assertThat(httpMessagesSent, hasSize(greaterThan(0)));
    }

    @Test
    void shouldReportXssWeaknessInJsonResponse() throws NullPointerException, IOException {
        String test = "/shouldReportXssInJsonReponse/";

        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String name = getFirstParamValue(session, "name");
                        String response;
                        if (name != null) {
                            response = getHtml("example.json", new String[][] {{"name", name}});
                        } else {
                            response = getHtml("example.json");
                        }
                        Response resp = newFixedLengthResponse(response);
                        resp.setMimeType("application/json");
                        return resp;
                    }
                });

        HttpMessage msg = this.getHttpMessage(test + "?name=test");
        this.rule.init(msg, this.parent);
        this.rule.scan();

        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(alertsRaised.get(0).getName(), containsString("JSON"));
        assertThat(alertsRaised.get(0).getParam(), equalTo("name"));
        assertThat(alertsRaised.get(0).getAttack(), equalTo("<script>alert(1);</script>"));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_LOW));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_LOW));
    }

    @Test
    void shouldReportXssInsideDivWithFilteredScript() throws NullPointerException, IOException {
        String test = "/shouldReportXssInBodyWithFilteredScript/";

        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String name = getFirstParamValue(session, "name");
                        String response;
                        if (name != null) {
                            // Strip out 'script' ignoring the case
                            name = name.replaceAll("(?i)script", "");
                            response =
                                    getHtml("InputInsideDiv.html", new String[][] {{"name", name}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });

        HttpMessage msg = this.getHttpMessage(test + "?name=test");

        this.rule.init(msg, this.parent);

        this.rule.scan();

        assertThat(httpMessagesSent, hasSize(equalTo(4)));
        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(alertsRaised.get(0).getEvidence(), equalTo("<img src=x onerror=alert(1);>"));
        assertThat(alertsRaised.get(0).getParam(), equalTo("name"));
        assertThat(alertsRaised.get(0).getAttack(), equalTo("<img src=x onerror=alert(1);>"));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_HIGH));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_MEDIUM));
    }

    @Test
    void shouldReportXssInBodyWithDoubleDecodedFilteredInjectionPointViaUrlParam()
            throws NullPointerException, IOException {
        String test = "/shouldReportXssInBodyWithFilteredScript/";

        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String name = getFirstParamValue(session, "name");
                        String response;
                        if (name != null) {
                            // Strip out 'script' ignoring the case
                            try {
                                // Only need to decode once more, server returns value decoded
                                name =
                                        URLDecoder.decode(
                                                name.replaceAll("(?i)(<|</)[0-9a-z ();=]+>", ""),
                                                "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                // Ignore
                            }
                            response = getHtml("InputInBody.html", new String[][] {{"name", name}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });

        HttpMessage msg = this.getHttpMessage(test + "?name=test");

        this.scannerParam.setTargetParamsInjectable(ScannerParam.TARGET_QUERYSTRING);
        this.rule.init(msg, this.parent);

        this.rule.scan();
        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(alertsRaised.get(0).getEvidence(), equalTo("<script>alert(1);</script>"));
        assertThat(alertsRaised.get(0).getParam(), equalTo("name"));
        assertThat(
                alertsRaised.get(0).getAttack(),
                equalTo("%253Cscript%253Ealert%25281%2529%253B%253C%252Fscript%253E"));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_HIGH));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_MEDIUM));
    }

    @Test
    void shouldNotAlertXssInBodyWithDoubleDecodedFilteredInjectionPointViaHeaderParam()
            throws NullPointerException, IOException {
        String test = "/shouldReportXssInBodyWithFilteredScript/";

        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String name = getFirstParamValue(session, "name");
                        String response;
                        if (name != null) {
                            // Strip out 'script' ignoring the case
                            try {
                                // Only need to decode once more, server returns value decoded
                                name =
                                        URLDecoder.decode(
                                                name.replaceAll("(?i)(<|</)[0-9a-z ();=]+>", ""),
                                                "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                // Ignore
                            }
                            response = getHtml("InputInBody.html", new String[][] {{"name", name}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });

        HttpMessage msg = this.getHttpMessage(test + "?name=test");

        this.scannerParam.setTargetParamsInjectable(ScannerParam.TARGET_HTTPHEADERS);
        this.rule.init(msg, this.parent);

        this.rule.scan();
        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    void shouldReportXssInBodyWithDoubleDecodedFilteredInjectionPointViaPostParam()
            throws NullPointerException, IOException {
        String test = "/shouldReportXssInBodyWithFilteredScript/";

        this.nano.addHandler(
                new NanoServerHandler(test) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        String sess = getBody(session);
                        String name = sess.split("=")[1];
                        String response;
                        if (name != null) {
                            // Strip out 'script' ignoring the case
                            try {
                                name =
                                        URLDecoder.decode(
                                                URLDecoder.decode(name, "UTF-8")
                                                        .replaceAll(
                                                                "(?i)(<|</)[0-9a-z ();=]+>", ""),
                                                "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                // Ignore
                            }
                            response = getHtml("InputInBody.html", new String[][] {{"name", name}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });

        HttpMessage msg = this.getHttpMessage(HttpRequestHeader.POST, test, "<html>/<html>");
        HtmlParameter param = new HtmlParameter(HtmlParameter.Type.form, "name", "test");
        TreeSet<HtmlParameter> paramSet = new TreeSet<>();
        paramSet.add(param);
        msg.setFormParams(paramSet);
        msg.getRequestHeader()
                .addHeader(
                        HttpRequestHeader.CONTENT_TYPE,
                        HttpRequestHeader.FORM_URLENCODED_CONTENT_TYPE);
        msg.getRequestHeader().setContentLength(msg.getRequestBody().length());

        this.scannerParam.setTargetParamsInjectable(ScannerParam.TARGET_POSTDATA);
        this.rule.init(msg, this.parent);

        this.rule.scan();
        assertThat(alertsRaised.size(), equalTo(1));
        assertThat(alertsRaised.get(0).getEvidence(), equalTo("<script>alert(1);</script>"));
        assertThat(alertsRaised.get(0).getParam(), equalTo("name"));
        assertThat(
                alertsRaised.get(0).getAttack(),
                equalTo("%253Cscript%253Ealert%25281%2529%253B%253C%252Fscript%253E"));
        assertThat(alertsRaised.get(0).getRisk(), equalTo(Alert.RISK_HIGH));
        assertThat(alertsRaised.get(0).getConfidence(), equalTo(Alert.CONFIDENCE_MEDIUM));
    }

    @Test
    void shouldNotAlertXssInJsVariableWithEncoding() throws HttpMalformedHeaderException {
        // Given
        String path = "/user/search";
        this.nano.addHandler(
                new NanoServerHandler(path) {

                    @Override
                    protected Response serve(IHTTPSession session) {
                        String name = getFirstParamValue(session, "name");
                        String response;
                        if (name != null) {
                            name = name.replaceAll("\"", "&quot;");
                            response =
                                    getHtml("InputInScript.html", new String[][] {{"name", name}});
                        } else {
                            response = getHtml("NoInput.html");
                        }
                        return newFixedLengthResponse(response);
                    }
                });

        HttpMessage msg = this.getHttpMessage(path + "?name=test");
        this.rule.init(msg, this.parent);

        // When
        this.rule.scan();

        // Then
        assertThat(alertsRaised.size(), equalTo(0));
    }

    @Test
    void shouldAlertOnceWithMultipleContexts() throws HttpMalformedHeaderException {
        // Given
        String path = "/api/search";
        this.nano.addHandler(
                new NanoServerHandler(path) {

                    @Override
                    protected Response serve(IHTTPSession session) {
                        String name = getFirstParamValue(session, "name");
                        String response;
                        if (name != null) {
                            response =
                                    getHtml("MultipleInput.html", new String[][] {{"name", name}});
                        } else {
                            response = getHtml("NoInput.html");
                        }

                        return newFixedLengthResponse(response);
                    }
                });

        HttpMessage msg = this.getHttpMessage(path + "?name=test");
        this.rule.init(msg, this.parent);

        // When
        this.rule.scan();

        // Then
        assertThat(alertsRaised.size(), equalTo(1));
    }

    @Override
    protected Path getResourcePath(String resourcePath) {
        return super.getResourcePath("crosssitescriptingscanrule/" + resourcePath);
    }
}
