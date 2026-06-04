import { Activity, Clock3, UserRound } from 'lucide-react';

export default function StatusPanel({ apiEvents, customerId }) {
  return (
    <aside className="statusPanel" aria-label="상태 관찰 패널">
      <div className="panelHeader">
        <Activity aria-hidden="true" size={18} />
        <h2>상태 관찰</h2>
      </div>

      <dl className="stateList">
        <div>
          <dt>
            <UserRound aria-hidden="true" size={16} />
            고객 ID
          </dt>
          <dd>{customerId}</dd>
        </div>
        <div>
          <dt>
            <Clock3 aria-hidden="true" size={16} />
            최근 API
          </dt>
          <dd>{apiEvents.length ? `${apiEvents.length}건` : '대기'}</dd>
        </div>
      </dl>

      <div className="apiTimeline">
        {apiEvents.length === 0 ? (
          <p className="mutedText">호출 기록이 아직 없어요.</p>
        ) : (
          apiEvents.map((event) => (
            <div className="apiEvent" key={event.id}>
              <span className={event.ok ? 'okDot' : 'failDot'} />
              <div>
                <strong>{event.method} {event.path}</strong>
                <p>{event.status} · {event.durationMs}ms · {event.at}</p>
              </div>
            </div>
          ))
        )}
      </div>
    </aside>
  );
}
