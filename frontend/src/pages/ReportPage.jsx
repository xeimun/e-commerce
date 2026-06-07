import {
  AlertCircle,
  BarChart3,
  CheckCircle2,
  ShieldCheck,
  Timer,
  TrendingDown,
  TrendingUp
} from 'lucide-react';

const report = {
  title: '인기 상품 주문 생성과 재고 차감',
  completedCount: 1,
  measuredAt: '2026-06-07',
  headline: '초과 판매 0건을 유지하면서 p95 응답 시간을 1,097.51ms에서 962.91ms로 개선',
  context: [
    '한정판 굿즈 판매 시작 직후 여러 고객이 같은 상품을 동시에 주문하는 상황',
    '쿠폰 없는 단건 주문으로 stocks hot row의 재고 차감 병목만 분리',
    '상품 10001, 재고 1000개, 주문 생성 500건, 동시성 100 조건'
  ],
  bottleneck: [
    '기존 흐름은 stocks 행을 PESSIMISTIC_WRITE로 조회한 뒤 재고 검증과 차감을 처리했다.',
    '같은 상품에 요청이 몰리면 하나의 row-level lock을 기준으로 뒤 요청이 대기한다.',
    '재고를 잠근 뒤 주문 상품 생성과 주문 저장까지 이어져 lock hold time이 응답 지연에 영향을 준다.'
  ],
  interviewSummary: [
    '인기 상품 주문 집중 상황에서 비관적 락 기반 재고 차감을 조건부 업데이트로 바꾸어 초과 판매 0건을 유지했다.',
    '로컬 동시성 100 측정에서 주문 생성 p95 응답 시간을 1,097.51ms에서 962.91ms로 낮추고 처리량을 5,647.27건/분에서 7,114.73건/분으로 개선했다.'
  ]
};

const throughputMetric = {
  label: '처리량',
  unit: '건/분',
  before: 5647.27,
  after: 7114.73,
  direction: 'higher'
};

const latencyMetrics = [
  {
    label: '평균',
    unit: 'ms',
    before: 963.63,
    after: 763.62,
    direction: 'lower'
  },
  {
    label: 'p95',
    unit: 'ms',
    before: 1097.51,
    after: 962.91,
    direction: 'lower'
  },
  {
    label: 'p99',
    unit: 'ms',
    before: 1117.01,
    after: 1077.76,
    direction: 'lower'
  }
];

const consistencyChecks = [
  { label: '충분한 재고 조건', result: '주문 500건 성공, 실패 0건' },
  { label: '최종 재고', result: '기대 재고 500, 실제 재고 500' },
  { label: '재고 소진 조건', result: '주문 200건 중 성공 100건, 실패 100건' },
  { label: '초과 판매', result: '0건' },
  { label: '실패 고객 주문 저장 흔적', result: '없음' }
];

const limitations = [
  '로컬 개발 장비와 Docker Compose 환경에서 측정했다.',
  'k6가 설치되어 있지 않아 Node.js 내장 fetch 기반 스크립트로 측정했다.',
  '기동 직후 첫 고부하 재측정 값이 튀어 반복 측정의 필요성이 있다.',
  'DB 락 대기 시간과 커넥션 풀 대기 시간은 별도 지표로 수집하지 못했다.',
  '부하 테스트 데이터가 누적되면 주문 저장 비용이 달라질 수 있다.'
];

function formatMetric(value, unit = '') {
  return `${value.toLocaleString('ko-KR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })}${unit}`;
}

function getChangePercent(metric) {
  const change = ((metric.after - metric.before) / metric.before) * 100;
  return metric.direction === 'lower' ? Math.abs(change) : change;
}

function MetricCard({ icon: Icon, label, tone = 'neutral', value, subline }) {
  return (
    <article className={`reportKpiCard ${tone}`}>
      <div className="reportKpiIcon" aria-hidden="true">
        <Icon size={20} />
      </div>
      <div>
        <span>{label}</span>
        <strong>{value}</strong>
        <p>{subline}</p>
      </div>
    </article>
  );
}

function ThroughputChart({ metric }) {
  const maxValue = Math.max(metric.before, metric.after);
  const beforePercent = (metric.before / maxValue) * 100;
  const afterPercent = (metric.after / maxValue) * 100;
  const changePercent = getChangePercent(metric);

  return (
    <section className="reportCard" aria-labelledby="throughput-title">
      <div className="sectionTitle">
        <h2 id="throughput-title">처리량</h2>
        <span>높을수록 좋음</span>
      </div>
      <div
        className="metricDelta success"
        aria-label={`처리량 ${changePercent.toFixed(1)}퍼센트 증가`}
      >
        <TrendingUp aria-hidden="true" size={18} />
        <strong>{changePercent.toFixed(1)}% 증가</strong>
      </div>
      <div className="barCompareChart">
        <div className="barCompareRow">
          <span>개선 전</span>
          <div className="barTrack" aria-hidden="true">
            <div
              className="barFill before"
              style={{ '--bar-size': `${beforePercent}%` }}
            />
          </div>
          <strong>{formatMetric(metric.before, metric.unit)}</strong>
        </div>
        <div className="barCompareRow">
          <span>개선 후</span>
          <div className="barTrack" aria-hidden="true">
            <div
              className="barFill after positive"
              style={{ '--bar-size': `${afterPercent}%` }}
            />
          </div>
          <strong>{formatMetric(metric.after, metric.unit)}</strong>
        </div>
      </div>
    </section>
  );
}

function LatencyChart({ metrics }) {
  const maxValue = Math.max(...metrics.flatMap((metric) => [metric.before, metric.after]));

  return (
    <section className="reportCard" aria-labelledby="latency-title">
      <div className="sectionTitle">
        <h2 id="latency-title">응답 시간</h2>
        <span>낮을수록 좋음</span>
      </div>
      <div className="latencyChart">
        {metrics.map((metric) => {
          const beforePercent = (metric.before / maxValue) * 100;
          const afterPercent = (metric.after / maxValue) * 100;
          const changePercent = getChangePercent(metric);

          return (
            <article className="latencyMetric" key={metric.label}>
              <div className="latencyMetricHeader">
                <div>
                  <strong>{metric.label}</strong>
                  <span>{changePercent.toFixed(1)}% 감소</span>
                </div>
                <TrendingDown aria-hidden="true" size={18} />
              </div>
              <div className="latencyBars">
                <div className="latencyBarGroup">
                  <div className="latencyBar before" style={{ '--bar-size': `${beforePercent}%` }}>
                    <span>{formatMetric(metric.before, metric.unit)}</span>
                  </div>
                  <small>개선 전</small>
                </div>
                <div className="latencyBarGroup">
                  <div className="latencyBar after" style={{ '--bar-size': `${afterPercent}%` }}>
                    <span>{formatMetric(metric.after, metric.unit)}</span>
                  </div>
                  <small>개선 후</small>
                </div>
              </div>
            </article>
          );
        })}
      </div>
    </section>
  );
}

function BulletSection({ icon: Icon, items, title }) {
  return (
    <section className="reportCard">
      <div className="panelHeader">
        <Icon aria-hidden="true" size={18} />
        <h2>{title}</h2>
      </div>
      <ul className="reportBulletList">
        {items.map((item) => (
          <li key={item}>{item}</li>
        ))}
      </ul>
    </section>
  );
}

export default function ReportPage() {
  const throughputChange = getChangePercent(throughputMetric);
  const p95Metric = latencyMetrics.find((metric) => metric.label === 'p95');
  const p95Change = getChangePercent(p95Metric);

  return (
    <section className="pageStack reportPage">
      <div className="pageHeader">
        <p className="eyebrow">Performance Report</p>
        <div>
          <h1>성능 리포트</h1>
          <p>완료된 성능 개선의 측정 조건, 병목 가설, 개선 전후 수치와 정합성 검증을 함께 확인합니다.</p>
        </div>
      </div>

      <section className="reportHero" aria-labelledby="report-hero-title">
        <div className="reportHeroCopy">
          <span>첫 번째 개선 결과</span>
          <h2 id="report-hero-title">{report.headline}</h2>
          <p>
            PostgreSQL 조건부 업데이트로 재고 검증과 차감을 하나의 DB 문장에 묶어
            재고 차감 임계 구역을 줄였습니다.
          </p>
        </div>
        <div className="reportHeroBadge" aria-label="측정 일자">
          <BarChart3 aria-hidden="true" size={22} />
          <span>측정일</span>
          <strong>{report.measuredAt}</strong>
        </div>
      </section>

      <div className="reportKpiGrid">
        <MetricCard
          icon={TrendingUp}
          label="처리량"
          tone="success"
          value={`${throughputChange.toFixed(1)}% 증가`}
          subline={`${formatMetric(throughputMetric.before, throughputMetric.unit)} → ${formatMetric(throughputMetric.after, throughputMetric.unit)}`}
        />
        <MetricCard
          icon={Timer}
          label="p95 응답 시간"
          tone="info"
          value={`${p95Change.toFixed(1)}% 감소`}
          subline={`${formatMetric(p95Metric.before, p95Metric.unit)} → ${formatMetric(p95Metric.after, p95Metric.unit)}`}
        />
        <MetricCard
          icon={ShieldCheck}
          label="초과 판매"
          tone="success"
          value="0건"
          subline="재고 소진 조건에서도 초과 판매 없음"
        />
        <MetricCard
          icon={CheckCircle2}
          label="완료된 개선"
          value={`${report.completedCount}건`}
          subline="완료된 성능 개선만 리포트에 노출"
        />
      </div>

      <div className="reportTabs" role="tablist" aria-label="성능 개선 리포트">
        <button className="segmentedTab active" type="button" role="tab" aria-selected="true">
          인기 상품 주문 생성
        </button>
      </div>

      <div className="reportDetailGrid">
        <div className="reportDetailMain">
          <BulletSection icon={BarChart3} title="상황" items={report.context} />
          <BulletSection icon={Timer} title="병목 가설" items={report.bottleneck} />

          <div className="reportChartGrid">
            <ThroughputChart metric={throughputMetric} />
            <LatencyChart metrics={latencyMetrics} />
          </div>

          <section className="reportCard" aria-labelledby="consistency-title">
            <div className="panelHeader">
              <ShieldCheck aria-hidden="true" size={18} />
              <h2 id="consistency-title">정합성 검증</h2>
            </div>
            <div className="consistencyGrid">
              {consistencyChecks.map((check) => (
                <div className="consistencyItem" key={check.label}>
                  <CheckCircle2 aria-hidden="true" size={18} />
                  <span>{check.label}</span>
                  <strong>{check.result}</strong>
                </div>
              ))}
            </div>
          </section>
        </div>

        <aside className="reportAside">
          <section className="reportCard">
            <div className="panelHeader">
              <AlertCircle aria-hidden="true" size={18} />
              <h2>한계</h2>
            </div>
            <ul className="reportLimitList">
              {limitations.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </section>

          <section className="reportCard interviewSummary">
            <div className="panelHeader">
              <CheckCircle2 aria-hidden="true" size={18} />
              <h2>면접용 요약</h2>
            </div>
            {report.interviewSummary.map((item) => (
              <p key={item}>{item}</p>
            ))}
          </section>
        </aside>
      </div>
    </section>
  );
}
