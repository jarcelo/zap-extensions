# Changelog
All notable changes to this add-on will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## Unreleased
### Added
- Support for custom messages in outputSummary job.
- Alert tags to the modern and traditional-plus reports.

### Changed
- Promoted to release.
- Minimum ZAP version 2.11.0

### Fixed
- Ignore false positives when listing messages in the outputSummary job.
- Bug in report job add-on which prevented the right theme from being used
- Added missing Modern template i18n messages

## [0.6.0] - 2021-09-16
### Fixed
- Address errors when running the OutputSummary job with Automation Framework.
- Alert counts to ignore false positives.

### Changed
- Maintenance changes.

## [0.5.0] - 2021-08-05
### Added
- Automation Framework 'theme' parameter.
- Automation Framework GUI

### Changed
- Maintenance changes.

## [0.4.0] - 2021-06-28
### Added
- Wappalyzer data to the traditional-html-plus report, if it is available.
- Traditional JSON report
- Automation job: outputSummary, aimed at mimicking the output of the packaged scans.
- Modern template - submitted via the ZAP Reporting Competition
- Parameter data to the traditional-html-plus report
- Methods to make it easier to generate reports from other add-ons

### Changed
- Maintenance changes.
- Handle multiple context URLs in automation.
- Traditional plus report - link to zaproxy.org pages for passing scan rules.
- Update links to repository.

### Fixed
- Include all relevant alerts in XML report templates (Issue 6627).
- Made XML reports more backwards compatible and fixed issue with generating it via the API.
- Issue with reports for sites with trailing slashes

## [0.3.0] - 2021-05-06
### Added
- API Support.
- Support for statistics

### Changed
- Maintenance Changes.
- Promote to beta

### Fixed
- Correct logging of dependency.
- Inconsistencies between traditional reports and the 'old' core ones
- Do not rely on default encoding when creating the reports, use UTF-8 always (Issue 6561).

## [0.2.0] - 2021-04-12

### Added
- Support for template sections
- Automation job: support risk, confidence and section configuration
- Passing rules to traditional plus HTML report

### Changed
- Format HTML and XML templates as part of the build

## [0.1.0] - 2021-03-19

### Added
- Support for resources such as JavaScript and images
- Support for template specific i18n property files
- Reload templates on dir change and handle no reports

## [0.0.1] - 2021-03-09

- First version.

[0.6.0]: https://github.com/zaproxy/zap-extensions/releases/reports-v0.6.0
[0.5.0]: https://github.com/zaproxy/zap-extensions/releases/reports-v0.5.0
[0.4.0]: https://github.com/zaproxy/zap-extensions/releases/reports-v0.4.0
[0.3.0]: https://github.com/zaproxy/zap-extensions/releases/reports-v0.3.0
[0.2.0]: https://github.com/zaproxy/zap-extensions/releases/reports-v0.2.0
[0.1.0]: https://github.com/zaproxy/zap-extensions/releases/reports-v0.1.0
[0.0.1]: https://github.com/zaproxy/zap-extensions/releases/reports-v0.0.1
