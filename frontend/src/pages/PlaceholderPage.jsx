export default function PlaceholderPage({ title }) {
  return (
    <section className="pageStack">
      <div className="pageHeader">
        <p className="eyebrow">Next</p>
        <div>
          <h1>{title}</h1>
          <p>다음 프론트 기능에서 이어서 연결합니다.</p>
        </div>
      </div>
    </section>
  );
}
