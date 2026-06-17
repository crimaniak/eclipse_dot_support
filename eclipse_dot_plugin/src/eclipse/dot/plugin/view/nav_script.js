// Graph viewer navigation module.
// Injected into every browser page by DotGraphView.buildHtml().
//
// Layout contract:
//   #vp  – full-viewport overflow:hidden container
//   #ct  – content wrapper; all transforms applied here
//
// Mouse bindings:
//   wheel        – zoom in/out keeping the cursor position fixed
//   left-drag    – pan (suppresses the trailing click)
//   left-click   – zoom in 25%, moving clicked point to viewport centre
//   right-click  – zoom out 25%, moving clicked point to viewport centre
//   double-click – fit entire content to viewport
var nav = (function () {
    var sc = 1, tx = 0, ty = 0;
    var dr = false, dsx, dsy, dtx, dty, wasDrag = false;

    function vp() { return document.getElementById('vp'); }
    function ct() { return document.getElementById('ct'); }

    // RAF render loop — caps DOM writes to one per display frame.
    // Every state mutation calls apply() which sets dirty=true;
    // the loop performs the single transform flush per frame.
    var dirty = false;
    (function loop() {
        if (dirty) {
            ct().style.transform = 'translate(' + tx + 'px,' + ty + 'px) scale(' + sc + ')';
            dirty = false;
        }
        requestAnimationFrame(loop);
    })();
    function apply() { dirty = true; }

    // Zoom keeping (mx, my) fixed in viewport space — used for wheel.
    function zAt(f, mx, my) {
        var ps = sc;
        sc = Math.max(0.05, Math.min(sc * f, 20));
        var af = sc / ps;
        tx = mx - (mx - tx) * af;
        ty = my - (my - ty) * af;
        apply();
    }

    // Zoom moving the content point currently at (mx, my) to viewport centre — used for clicks.
    function centerAt(f, mx, my) {
        var ps = sc;
        sc = Math.max(0.05, Math.min(sc * f, 20));
        var af = sc / ps;
        var v = vp();
        tx = v.clientWidth  / 2 - (mx - tx) * af;
        ty = v.clientHeight / 2 - (my - ty) * af;
        apply();
    }

    function fit() {
        var v = vp(), c = ct();
        var vw = v.clientWidth, vh = v.clientHeight;
        var cw = c.scrollWidth, ch = c.scrollHeight;
        if (cw > 0 && ch > 0) {
            sc = Math.min(vw / cw, vh / ch);
            tx = (vw - cw * sc) / 2;
            ty = (vh - ch * sc) / 2;
        }
        apply();
    }

    var v = vp();

    v.addEventListener('wheel', function (e) {
        e.preventDefault();
        zAt(e.deltaY < 0 ? 1.1 : 1 / 1.1, e.clientX, e.clientY);
    }, { passive: false });

    v.addEventListener('mousedown', function (e) {
        if (e.button !== 0) return;
        dr = true; wasDrag = false;
        dsx = e.clientX; dsy = e.clientY; dtx = tx; dty = ty;
        e.preventDefault();
    });
    window.addEventListener('mousemove', function (e) {
        if (!dr) return;
        var dx = e.clientX - dsx, dy = e.clientY - dsy;
        if (Math.abs(dx) > 3 || Math.abs(dy) > 3) wasDrag = true;
        tx = dtx + dx; ty = dty + dy; apply();
    });
    window.addEventListener('mouseup', function () { dr = false; });

    v.addEventListener('click', function (e) {
        if (wasDrag) { wasDrag = false; return; }
        centerAt(1.25, e.clientX, e.clientY);
    });

    // dblclick fires after two click events; fit() overrides their zoom in the same turn.
    v.addEventListener('dblclick', function () { fit(); });

    v.addEventListener('contextmenu', function (e) {
        e.preventDefault();
        centerAt(1 / 1.25, e.clientX, e.clientY);
    });

    // Initial fit; window.load catches deferred PNG decoding.
    fit();
    window.addEventListener('load', fit);

    return {
        zoomBy:  function (f) { var v = vp(); zAt(f, v.clientWidth / 2, v.clientHeight / 2); },
        zoomFit: fit
    };
})();
