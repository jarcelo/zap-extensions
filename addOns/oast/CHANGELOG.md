# Changelog

All notable changes to this add-on will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased
### Changed
- Updated the default Interactsh server URL to https://interactsh.com.

## [0.4.0] - 2021-09-22
### Added
- Interactsh support.

### Changed
- The _OAST Register Request Handler.js_ script template now also prints the raw request sent to the server.

## [0.3.0] - 2021-08-26
### Added
- A "Poll Now" button to the OAST tab.

## [0.2.2] - 2021-08-23
### Fixed
- The add-on did not stop when ZAP did, which led to ZAP hanging.

### Changed
- Minor script and help updates.

## [0.2.1] - 2021-08-19
### Changed
- Renamed the "OAST Callbacks" tab to "OAST".
- Updated help pages.

### Fixed
- Script templates were being loaded twice, resulting in a warning.

## [0.2.0] - 2021-08-17
### Added
- An option to allow changing the polling frequency of BOAST servers.
- A table that lists the payloads and canary values of all registered BOAST servers.
- Two new scripts that demonstrate how to interact with this add-on:
  - OAST Register Request Handler.js (Template)
  - OAST Get BOAST Servers.js

### Removed
- The _ID_ and the _Canary Value_ fields, in favour of the _Active Servers_ table in the BOAST options window.

## [0.1.1] - 2021-08-04
### Fixed
- Requests were not showing up in the OAST Callbacks panel.
- BOAST servers were not being polled after registration.

## [0.1.0] - 2021-08-04

[0.4.0]: https://github.com/zaproxy/zap-extensions/releases/oast-v0.4.0
[0.3.0]: https://github.com/zaproxy/zap-extensions/releases/oast-v0.3.0
[0.2.2]: https://github.com/zaproxy/zap-extensions/releases/oast-v0.2.2
[0.2.1]: https://github.com/zaproxy/zap-extensions/releases/oast-v0.2.1
[0.2.0]: https://github.com/zaproxy/zap-extensions/releases/oast-v0.2.0
[0.1.1]: https://github.com/zaproxy/zap-extensions/releases/oast-v0.1.1
[0.1.0]: https://github.com/zaproxy/zap-extensions/releases/oast-v0.1.0
