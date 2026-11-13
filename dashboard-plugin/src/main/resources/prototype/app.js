/* ALGER Phase 0 prototype — throwaway design artefact (never published). */
(function () {
    'use strict';

    var MOCK_URL = 'mock/dashboard-data.mock.json';
    var state = new URLSearchParams(window.location.search).get('state') || 'loaded';

    function esc(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function percent(borough) {
        var total = Number(borough.pointsTotal || 0);
        if (!total) return 0;
        return Math.max(0, Math.min(100, Math.round((Number(borough.pointsDone || 0) * 100) / total)));
    }

    function dagLevel(borough) {
        return borough.dag || 'N?';
    }

    function loadMock() {
        if (state === 'empty') {
            return Promise.resolve({ stats: {}, criticalPath: [], boroughs: [], sessions: [] });
        }
        return fetch(MOCK_URL)
            .then(function (response) {
                if (!response.ok) throw new Error('mock not found');
                return response.json();
            })
            .then(function (data) {
                if (state === 'overloaded') {
                    data = overloaded(data);
                }
                return data;
            });
    }

    function overloaded(data) {
        var copy = JSON.parse(JSON.stringify(data));
        var source = copy.boroughs || [];
        var expanded = [];
        for (var i = 0; i < 120; i += 1) {
            var base = source[i % source.length] || { name: 'Borough', project: 'x-gradle', dag: 'N2' };
            expanded.push(Object.assign({}, base, {
                name: base.name + ' #' + (i + 1),
                pointsDone: Math.round((base.pointsDone || 0) * ((i % 5) + 1) / 5),
                pointsTotal: base.pointsTotal || 10
            }));
        }
        copy.boroughs = expanded;
        return copy;
    }

    function emptyState(message) {
        return '<div class="empty-state">' + esc(message) + '</div>';
    }

    function renderPanorama(data) {
        var path = document.getElementById('critical-path');
        var progress = document.getElementById('progress-list');
        var dag = document.getElementById('dag-levels');
        if (!path || !progress || !dag) return;

        var rings = data.criticalPath || [];
        if (!rings.length) {
            path.innerHTML = emptyState('No critical path declared.');
        } else {
            path.innerHTML = rings.map(function (ring) {
                var pct = Math.max(0, Math.min(100, Number(ring.progress || 0)));
                return '<li data-state="' + esc(ring.state) + '">' +
                    '<span class="ring">' + esc(ring.ring) + '</span>' + esc(ring.label) +
                    ' <span class="who">— ' + esc(ring.borough) + '</span>' +
                    '<div class="mini"><span style="width:' + pct + '%"></span></div>' +
                    '</li>';
            }).join('');
        }

        var boroughs = (data.boroughs || []).slice().sort(function (a, b) {
            return percent(b) - percent(a);
        });
        if (!boroughs.length) {
            progress.innerHTML = emptyState('No borough to display.');
        } else {
            progress.innerHTML = boroughs.map(function (borough) {
                var pct = percent(borough);
                return '<div class="bar-row">' +
                    '<span class="name" title="' + esc(borough.name) + '">' + esc(borough.name) + '</span>' +
                    '<span class="bar"><span style="width:' + pct + '%"></span></span>' +
                    '<span class="pct">' + pct + '%</span>' +
                    '</div>';
            }).join('');
        }

        var levels = {};
        boroughs.forEach(function (borough) {
            var level = dagLevel(borough);
            (levels[level] = levels[level] || []).push(borough);
        });
        var levelNames = Object.keys(levels).sort();
        if (!levelNames.length) {
            dag.innerHTML = emptyState('No DAG level to display.');
        } else {
            dag.innerHTML = levelNames.map(function (level) {
                return '<div class="dag-level"><h3>' + esc(level) + '</h3><div class="chips">' +
                    levels[level]
                        .sort(function (a, b) { return String(a.project).localeCompare(String(b.project)); })
                        .map(function (borough) {
                            return '<a class="chip" data-state="' + esc(borough.state || 'active') + '" ' +
                                'href="borough.html?id=' + encodeURIComponent(borough.name) + '">' +
                                '<span class="dot"></span>' + esc(borough.name) + '</a>';
                        })
                        .join('') +
                    '</div></div>';
            }).join('');
        }
    }

    function renderBorough(data) {
        var sheet = document.getElementById('borough-sheet');
        if (!sheet) return;
        var id = new URLSearchParams(window.location.search).get('id') || (data.boroughs[0] || {}).name;
        var borough = (data.boroughs || []).filter(function (candidate) { return candidate.name === id; })[0];
        if (!borough) {
            sheet.innerHTML = '<h1>' + esc(id) + '</h1>' + emptyState('Unknown borough in the mock data.');
            return;
        }
        var pct = percent(borough);
        var sessions = (data.sessions || []).filter(function (session) { return session.borough === borough.name; });
        var sessionsHtml = sessions.length
            ? '<ul class="sessions">' + sessions.map(function (session) {
                return '<li><strong>S' + esc(session.number) + '</strong> — ' + esc(session.subject) +
                    '<div class="date">' + esc(session.date) + '</div></li>';
            }).join('') + '</ul>'
            : emptyState('No recent session.');

        sheet.innerHTML =
            '<h1>' + esc(borough.name) + '</h1>' +
            '<div class="sub">' + esc(borough.project) + ' — DAG ' + esc(dagLevel(borough)) + '</div>' +
            '<div class="grid-2">' +
            '<div class="card"><h3>Position</h3>' +
            '<dl class="kv">' +
            '<dt>Borough</dt><dd>' + esc(borough.name) + '</dd>' +
            '<dt>Project</dt><dd>' + esc(borough.project) + '</dd>' +
            '<dt>DAG layer</dt><dd>' + esc(dagLevel(borough)) + '</dd>' +
            '<dt>State</dt><dd>' + esc(borough.state || 'active') + '</dd>' +
            '<dt>Critical path</dt><dd>' + esc(borough.ring || '—') + '</dd>' +
            '</dl></div>' +
            '<div class="card"><h3>Progress</h3>' +
            '<dl class="kv">' +
            '<dt>Points</dt><dd>' + esc(borough.pointsDone) + ' / ' + esc(borough.pointsTotal) + '</dd>' +
            '<dt>Percent</dt><dd>' + pct + ' %</dd>' +
            '</dl>' +
            '<div class="bar"><span style="width:' + pct + '%"></span></div>' +
            '</div>' +
            '</div>' +
            '<div class="card" style="margin-top:1rem"><h3>Recent sessions</h3>' + sessionsHtml + '</div>' +
            '<div class="drilldown"><a href="index.html">← Panorama</a><a href="graph.html">Graph shell →</a></div>';
    }

    function renderGraph(data) {
        var columns = document.getElementById('graph-columns');
        var panel = document.getElementById('graph-panel');
        if (!columns || !panel) return;
        var boroughs = data.boroughs || [];
        if (!boroughs.length) {
            columns.innerHTML = emptyState('No node to display.');
            return;
        }
        var levels = ['N0', 'N1', 'N2', 'N3', 'N4'];
        var others = {};
        boroughs.forEach(function (borough) {
            var level = dagLevel(borough);
            if (levels.indexOf(level) === -1) {
                (others[level] = others[level] || []).push(borough);
            }
        });
        var allLevels = levels.concat(Object.keys(others));
        columns.innerHTML = allLevels.map(function (level) {
            var nodes = boroughs.filter(function (borough) { return dagLevel(borough) === level; });
            return '<div class="graph-column"><h4>' + esc(level) + '</h4>' +
                nodes.map(function (borough) {
                    return '<button class="node" data-id="' + esc(borough.name) + '">' +
                        esc(borough.name) + ' <span class="muted">' + percent(borough) + '%</span></button>';
                }).join('') +
                '</div>';
        }).join('');

        columns.addEventListener('click', function (event) {
            var button = event.target.closest('.node');
            if (!button) return;
            var selected = columns.querySelector('.node.selected');
            if (selected) selected.classList.remove('selected');
            button.classList.add('selected');
            var borough = boroughs.filter(function (candidate) { return candidate.name === button.dataset.id; })[0];
            if (!borough) return;
            panel.innerHTML =
                '<h3>' + esc(borough.name) + '</h3>' +
                '<p class="muted">' + esc(borough.project) + ' — ' + esc(dagLevel(borough)) + '</p>' +
                '<dl class="kv">' +
                '<dt>State</dt><dd>' + esc(borough.state || 'active') + '</dd>' +
                '<dt>Points</dt><dd>' + esc(borough.pointsDone) + ' / ' + esc(borough.pointsTotal) + '</dd>' +
                '<dt>Percent</dt><dd>' + percent(borough) + ' %</dd>' +
                '</dl>' +
                '<div class="drilldown"><a href="borough.html?id=' + encodeURIComponent(borough.name) + '">Open sheet →</a></div>';
        });
    }

    loadMock()
        .then(function (data) {
            var page = document.body.getAttribute('data-page');
            if (page === 'panorama') renderPanorama(data);
            if (page === 'borough') renderBorough(data);
            if (page === 'graph') renderGraph(data);
        })
        .catch(function (error) {
            var target = document.querySelector('main');
            if (target) {
                target.insertAdjacentHTML('afterbegin',
                    emptyState('Mock data unavailable (' + error.message + '). Serve this directory over HTTP.'));
            }
        });
})();
